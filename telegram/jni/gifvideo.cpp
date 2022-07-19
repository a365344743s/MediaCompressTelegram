#include <jni.h>
#include <string>
#include <unistd.h>
#include "c_utils.h"

extern "C" {
#include <libavformat/isom.h>
#include <libavcodec/bytestream.h>
#include <libavcodec/get_bits.h>
#include <libavutil/eval.h>
#include <libswscale/swscale.h>
}

JavaVM *javaVm = nullptr;

static const std::string av_make_error_str(int errnum) {
    char errbuf[AV_ERROR_MAX_STRING_SIZE];
    av_strerror(errnum, errbuf, AV_ERROR_MAX_STRING_SIZE);
    return (std::string) errbuf;
}

#undef av_err2str
#define av_err2str(errnum) av_make_error_str(errnum).c_str()
#define FFMPEG_AVSEEK_SIZE 0x10000

typedef struct H2645NAL {
    uint8_t *rbsp_buffer;
    int size;
    const uint8_t *data;
    int size_bits;
    int raw_size;
    const uint8_t *raw_data;
    int type;
    int temporal_id;
    int nuh_layer_id;
    int skipped_bytes;
    int skipped_bytes_pos_size;
    int *skipped_bytes_pos;
    int ref_idc;
    GetBitContext gb;
} H2645NAL;

typedef struct H2645RBSP {
    uint8_t *rbsp_buffer;
    AVBufferRef *rbsp_buffer_ref;
    int rbsp_buffer_alloc_size;
    int rbsp_buffer_size;
} H2645RBSP;

typedef struct H2645Packet {
    H2645NAL *nals;
    H2645RBSP rbsp;
    int nb_nals;
    int nals_allocated;
    unsigned nal_buffer_size;
} H2645Packet;

void ff_h2645_packet_uninit(H2645Packet *pkt) {
    int i;
    for (i = 0; i < pkt->nals_allocated; i++) {
        av_freep(&pkt->nals[i].skipped_bytes_pos);
    }
    av_freep(&pkt->nals);
    pkt->nals_allocated = pkt->nal_buffer_size = 0;
    if (pkt->rbsp.rbsp_buffer_ref) {
        av_buffer_unref(&pkt->rbsp.rbsp_buffer_ref);
        pkt->rbsp.rbsp_buffer = NULL;
    } else
        av_freep(&pkt->rbsp.rbsp_buffer);
    pkt->rbsp.rbsp_buffer_alloc_size = pkt->rbsp.rbsp_buffer_size = 0;
}

typedef struct VideoInfo {

    ~VideoInfo() {
        if (video_dec_ctx) {
            avcodec_close(video_dec_ctx);
            video_dec_ctx = nullptr;
        }
        if (fmt_ctx) {
            avformat_close_input(&fmt_ctx);
            fmt_ctx = nullptr;
        }
        if (frame) {
            av_frame_free(&frame);
            frame = nullptr;
        }
        if (src) {
            delete [] src;
            src = nullptr;
        }
        if (stream != nullptr) {
            JNIEnv *jniEnv = nullptr;
            JavaVMAttachArgs jvmArgs;
            jvmArgs.version = JNI_VERSION_1_6;

            bool attached;
            if (JNI_EDETACHED == javaVm->GetEnv((void **) &jniEnv, JNI_VERSION_1_6)) {
                javaVm->AttachCurrentThread(&jniEnv, &jvmArgs);
                attached = true;
            } else {
                attached = false;
            }
            jniEnv->DeleteGlobalRef(stream);
            if (attached) {
                javaVm->DetachCurrentThread();
            }
            stream = nullptr;
        }
        if (ioContext != nullptr) {
            if (ioContext->buffer) {
                av_freep(&ioContext->buffer);
            }
            avio_context_free(&ioContext);
            ioContext = nullptr;
        }
        if (sws_ctx != nullptr) {
            sws_freeContext(sws_ctx);
            sws_ctx = nullptr;
        }
        if (fd >= 0) {
            close(fd);
            fd = -1;
        }

        ff_h2645_packet_uninit(&h2645Packet);
        av_packet_unref(&orig_pkt);

        video_stream_idx = -1;
        video_stream = nullptr;
        audio_stream = nullptr;
    }

    AVFormatContext *fmt_ctx = nullptr;
    char *src = nullptr;
    int video_stream_idx = -1;
    AVStream *video_stream = nullptr;
    AVStream *audio_stream = nullptr;
    AVCodecContext *video_dec_ctx = nullptr;
    AVFrame *frame = nullptr;
    bool has_decoded_frames = false;
    AVPacket pkt;
    AVPacket orig_pkt;
    bool stopped = false;
    bool seeking = false;

    int firstWidth = 0;
    int firstHeight = 0;

    bool dropFrames = false;

    H2645Packet h2645Packet = {nullptr};

    int32_t dst_linesize[1];

    struct SwsContext *sws_ctx = nullptr;

    AVIOContext *ioContext = nullptr;
    unsigned char *ioBuffer = nullptr;
    jobject stream = nullptr;
    int32_t account = 0;
    int fd = -1;
    int64_t file_size = 0;
    int64_t last_seek_p = 0;
};

enum PARAM_NUM {
    PARAM_NUM_SUPPORTED_VIDEO_CODEC = 0,
    PARAM_NUM_WIDTH = 1,
    PARAM_NUM_HEIGHT = 2,
    PARAM_NUM_BITRATE = 3,
    PARAM_NUM_DURATION = 4,
    PARAM_NUM_AUDIO_FRAME_SIZE = 5,
    PARAM_NUM_VIDEO_FRAME_SIZE = 6,
    PARAM_NUM_FRAMERATE = 7,
    PARAM_NUM_ROTATION = 8,
    PARAM_NUM_SUPPORTED_AUDIO_CODEC = 9,
    PARAM_NUM_HAS_AUDIO = 10,
    PARAM_NUM_COUNT = 11,
};

extern "C" JNIEXPORT void JNICALL Java_org_telegram_ui_Components_AnimatedFileDrawable_getVideoInfo(JNIEnv *env, jclass clazz,jint sdkVersion, jstring src, jintArray data) {
    VideoInfo *info = new VideoInfo();

    char const *srcString = env->GetStringUTFChars(src, 0);
    size_t len = strlen(srcString);
    info->src = new char[len + 1];
    memcpy(info->src, srcString, len);
    info->src[len] = '\0';
    if (srcString != nullptr) {
        env->ReleaseStringUTFChars(src, srcString);
    }

    int ret;
    if ((ret = avformat_open_input(&info->fmt_ctx, info->src, NULL, NULL)) < 0) {
        LOGE("can't open source file %s, %s", info->src, av_err2str(ret));
        delete info;
        return;
    }

    if ((ret = avformat_find_stream_info(info->fmt_ctx, NULL)) < 0) {
        LOGE("can't find stream information %s, %s", info->src, av_err2str(ret));
        delete info;
        return;
    }

    if ((ret = av_find_best_stream(info->fmt_ctx, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0)) >= 0) {
        info->video_stream = info->fmt_ctx->streams[ret];
    }

    if ((ret = av_find_best_stream(info->fmt_ctx, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0)) >= 0) {
        info->audio_stream = info->fmt_ctx->streams[ret];
    }

    if (info->video_stream == nullptr) {
        LOGE("can't find video stream in the input, aborting %s", info->src);
        delete info;
        return;
    }

    jint *dataArr = env->GetIntArrayElements(data, 0);
    if (dataArr != nullptr) {
        //https://developer.android.com/guide/topics/media/media-formats
        dataArr[PARAM_NUM_SUPPORTED_VIDEO_CODEC] =
                info->video_stream->codecpar->codec_id == AV_CODEC_ID_H264 ||
                info->video_stream->codecpar->codec_id == AV_CODEC_ID_H263 ||
                info->video_stream->codecpar->codec_id == AV_CODEC_ID_MPEG4 ||
                info->video_stream->codecpar->codec_id == AV_CODEC_ID_VP8 ||
                info->video_stream->codecpar->codec_id == AV_CODEC_ID_VP9 ||
                (sdkVersion > 21 && info->video_stream->codecpar->codec_id == AV_CODEC_ID_HEVC);

        if (strstr(info->fmt_ctx->iformat->name, "mov") != 0 && dataArr[PARAM_NUM_SUPPORTED_VIDEO_CODEC]) {
            MOVStreamContext *mov = (MOVStreamContext *) info->video_stream->priv_data;
            dataArr[PARAM_NUM_VIDEO_FRAME_SIZE] = (jint) mov->data_size;

            if (info->audio_stream != nullptr) {
                mov = (MOVStreamContext *) info->audio_stream->priv_data;
                dataArr[PARAM_NUM_AUDIO_FRAME_SIZE] = (jint) mov->data_size;
            }
        }

        if (info->audio_stream != nullptr) {
            //https://developer.android.com/guide/topics/media/media-formats
            dataArr[PARAM_NUM_SUPPORTED_AUDIO_CODEC] =
                    info->audio_stream->codecpar->codec_id == AV_CODEC_ID_AAC ||
                    info->audio_stream->codecpar->codec_id == AV_CODEC_ID_AAC_LATM ||
                    info->audio_stream->codecpar->codec_id == AV_CODEC_ID_VORBIS ||
                    info->audio_stream->codecpar->codec_id == AV_CODEC_ID_AMR_NB ||
                    info->audio_stream->codecpar->codec_id == AV_CODEC_ID_AMR_WB ||
                    info->audio_stream->codecpar->codec_id == AV_CODEC_ID_FLAC ||
                    info->audio_stream->codecpar->codec_id == AV_CODEC_ID_MP3 ||
                    // not supported codec, skip audio in this case
                    info->audio_stream->codecpar->codec_id == AV_CODEC_ID_ADPCM_IMA_WAV ||
                    (sdkVersion > 21 && info->audio_stream->codecpar->codec_id == AV_CODEC_ID_OPUS);
            dataArr[PARAM_NUM_HAS_AUDIO] = 1;
        } else {
            dataArr[PARAM_NUM_HAS_AUDIO] = 0;
        }

        dataArr[PARAM_NUM_BITRATE] = (jint) info->video_stream->codecpar->bit_rate;
        dataArr[PARAM_NUM_WIDTH] = info->video_stream->codecpar->width;
        dataArr[PARAM_NUM_HEIGHT] = info->video_stream->codecpar->height;
        AVDictionaryEntry *rotate_tag = av_dict_get(info->video_stream->metadata, "rotate", NULL, 0);
        if (rotate_tag && *rotate_tag->value && strcmp(rotate_tag->value, "0") != 0) {
            char *tail;
            dataArr[PARAM_NUM_ROTATION] = (jint) av_strtod(rotate_tag->value, &tail);
            if (*tail) {
                dataArr[PARAM_NUM_ROTATION] = 0;
            }
        } else {
            dataArr[PARAM_NUM_ROTATION] = 0;
        }
        if (info->video_stream->codecpar->codec_id == AV_CODEC_ID_H264 || info->video_stream->codecpar->codec_id == AV_CODEC_ID_HEVC) {
            dataArr[PARAM_NUM_FRAMERATE] = (jint) av_q2d(info->video_stream->avg_frame_rate);
        } else {
            dataArr[PARAM_NUM_FRAMERATE] = (jint) av_q2d(info->video_stream->r_frame_rate);
        }
        dataArr[PARAM_NUM_DURATION] = (int32_t) (info->fmt_ctx->duration * 1000 / AV_TIME_BASE);
        env->ReleaseIntArrayElements(data, dataArr, 0);
        delete info;
    }
}

extern "C" jint videoOnJNILoad(JavaVM *vm, JNIEnv *env) {
    //av_log_set_callback(custom_log);
    javaVm = vm;

    return JNI_TRUE;
}
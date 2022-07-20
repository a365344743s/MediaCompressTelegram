package org.telegram.messenger;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import org.telegram.ui.Components.AnimatedFileDrawable;

import java.util.WeakHashMap;

import chengdu.ws.common.Scheduler;
import chengdu.ws.telegram.BuildConfig;

public class VideoConvertUtil {
    private static final String TAG = Utilities.class.getSimpleName();
    private static final WeakHashMap<Integer, VideoEditedInfo> sConvertInfoMap = new WeakHashMap<>();
    private static Scheduler sScheduler;

    /**
     * 初始化
     */
    public static void init(Scheduler scheduler) {
        sScheduler = scheduler;
    }

    public static Scheduler getScheduler() {
        return sScheduler;
    }

    /**
     * 开始视频转换
     *
     * @param videoPath  视频路径
     * @param attachPath 转换后路径
     * @param listener   回调
     * @return 唯一id
     */
    @Nullable
    public static Integer startVideoConvert(String videoPath, String attachPath, MediaController.ConvertorListener listener) {
        VideoEditedInfo info = createCompressionSettings(videoPath);
        if (info == null) {
            return null;
        }
        info.attachPath = attachPath;
        boolean ret = MediaController.getInstance().scheduleVideoConvert(info, listener);
        if (!ret) {
            return null;
        }
        sConvertInfoMap.put(info.id, info);
        return info.id;
    }

    /**
     * 停止视频转换
     *
     * @param id 唯一id
     */
    public static void stopVideoConvert(int id) {
        VideoEditedInfo info = sConvertInfoMap.remove(id);
        if (info != null) {
            MediaController.getInstance().cancelVideoConvert(info);
        }
    }

    /**
     * 压缩参数设置
     *
     * @param videoPath 视频路径
     */
    private static VideoEditedInfo createCompressionSettings(String videoPath) {
        int[] params = new int[AnimatedFileDrawable.PARAM_NUM_COUNT];
        AnimatedFileDrawable.getVideoInfo(videoPath, params);

        if (params[AnimatedFileDrawable.PARAM_NUM_SUPPORTED_VIDEO_CODEC] == 0) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "video hasn't avc1 atom");
            }
            return null;
        }

        int originalBitrate = getVideoBitrate(videoPath);
        if (originalBitrate == -1) {
            originalBitrate = params[AnimatedFileDrawable.PARAM_NUM_BITRATE];
        }
        int bitrate = originalBitrate;
        float videoDuration = params[AnimatedFileDrawable.PARAM_NUM_DURATION];
        int videoFramerate = params[AnimatedFileDrawable.PARAM_NUM_FRAMERATE];


        if (Build.VERSION.SDK_INT < 18) {
            try {
                MediaCodecInfo codecInfo = selectCodec(MediaController.VIDEO_MIME_TYPE);
                if (codecInfo == null) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "no codec info for " + MediaController.VIDEO_MIME_TYPE);
                    }
                    return null;
                } else {
                    String name = codecInfo.getName();
                    if (name.equals("OMX.google.h264.encoder") ||
                            name.equals("OMX.ST.VFM.H264Enc") ||
                            name.equals("OMX.Exynos.avc.enc") ||
                            name.equals("OMX.MARVELL.VIDEO.HW.CODA7542ENCODER") ||
                            name.equals("OMX.MARVELL.VIDEO.H264ENCODER") ||
                            name.equals("OMX.k3.video.encoder.avc") ||
                            name.equals("OMX.TI.DUCATI1.VIDEO.H264E")) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "unsupported encoder = " + name);
                        }
                        return null;
                    } else {
                        if (selectColorFormat(codecInfo, MediaController.VIDEO_MIME_TYPE) == 0) {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "no color format for " + MediaController.VIDEO_MIME_TYPE);
                            }
                            return null;
                        }
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }

        VideoEditedInfo videoEditedInfo = new VideoEditedInfo();
        videoEditedInfo.startTime = -1;
        videoEditedInfo.endTime = -1;
        videoEditedInfo.originalBitrate = videoEditedInfo.bitrate = bitrate;
        videoEditedInfo.originalPath = videoPath;
        videoEditedInfo.framerate = videoFramerate;
        videoEditedInfo.resultWidth = videoEditedInfo.originalWidth = params[AnimatedFileDrawable.PARAM_NUM_WIDTH];
        videoEditedInfo.resultHeight = videoEditedInfo.originalHeight = params[AnimatedFileDrawable.PARAM_NUM_HEIGHT];
        videoEditedInfo.rotationValue = params[AnimatedFileDrawable.PARAM_NUM_ROTATION];
        videoEditedInfo.originalDuration = (long) (videoDuration * 1000);

        int compressionsCount;

        float maxSize = Math.max(videoEditedInfo.originalWidth, videoEditedInfo.originalHeight);
        if (maxSize > 1280) {
            compressionsCount = 4;
        } else if (maxSize > 854) {
            compressionsCount = 3;
        } else if (maxSize > 640) {
            compressionsCount = 2;
        } else {
            compressionsCount = 1;
        }

        // WIFI || MOBILE
        int selectedCompression = Math.round(100 / (100f / compressionsCount));
        // ROAMING
//        int selectedCompression = Math.round(50 / (100f / compressionsCount));

        if (selectedCompression > compressionsCount) {
            selectedCompression = compressionsCount;
        }
        boolean needCompress = false;
        if (selectedCompression != compressionsCount || Math.max(videoEditedInfo.originalWidth, videoEditedInfo.originalHeight) > 1280) {
            needCompress = true;
            switch (selectedCompression) {
                case 1:
                    maxSize = 432.0f;
                    break;
                case 2:
                    maxSize = 640.0f;
                    break;
                case 3:
                    maxSize = 848.0f;
                    break;
                default:
                    maxSize = 1280.0f;
                    break;
            }
            float scale = videoEditedInfo.originalWidth > videoEditedInfo.originalHeight ? maxSize / videoEditedInfo.originalWidth : maxSize / videoEditedInfo.originalHeight;
            videoEditedInfo.resultWidth = Math.round(videoEditedInfo.originalWidth * scale / 2) * 2;
            videoEditedInfo.resultHeight = Math.round(videoEditedInfo.originalHeight * scale / 2) * 2;
        }
        bitrate = makeVideoBitrate(
                videoEditedInfo.originalHeight, videoEditedInfo.originalWidth,
                originalBitrate,
                videoEditedInfo.resultHeight, videoEditedInfo.resultWidth
        );


        if (!needCompress) {
            videoEditedInfo.resultWidth = videoEditedInfo.originalWidth;
            videoEditedInfo.resultHeight = videoEditedInfo.originalHeight;
            videoEditedInfo.bitrate = bitrate;
        } else {
            videoEditedInfo.bitrate = bitrate;
        }

        return videoEditedInfo;
    }

    private static int getVideoBitrate(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int bitrate = 0;
        try {
            retriever.setDataSource(path);
            bitrate = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        }

        retriever.release();
        return bitrate;
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo lastCodecInfo = null;
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    lastCodecInfo = codecInfo;
                    String name = lastCodecInfo.getName();
                    if (name != null) {
                        if (!name.equals("OMX.SEC.avc.enc")) {
                            return lastCodecInfo;
                        } else if (name.equals("OMX.SEC.AVC.Encoder")) {
                            return lastCodecInfo;
                        }
                    }
                }
            }
        }
        return lastCodecInfo;
    }

    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        int lastColorFormat = 0;
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                lastColorFormat = colorFormat;
                if (!(codecInfo.getName().equals("OMX.SEC.AVC.Encoder") && colorFormat == 19)) {
                    return colorFormat;
                }
            }
        }
        return lastColorFormat;
    }

    private static int makeVideoBitrate(int originalHeight, int originalWidth, int originalBitrate, int height, int width) {
        float compressFactor;
        float minCompressFactor;
        int maxBitrate;
        if (Math.min(height, width) >= 1080) {
            maxBitrate = 6800_000;
            compressFactor = 1f;
            minCompressFactor = 1f;
        } else if (Math.min(height, width) >= 720) {
            maxBitrate = 2600_000;
            compressFactor = 1f;
            minCompressFactor = 1f;
        } else if (Math.min(height, width) >= 480) {
            maxBitrate = 1000_000;
            compressFactor = 0.75f;
            minCompressFactor = 0.9f;
        } else {
            maxBitrate = 750_000;
            compressFactor = 0.6f;
            minCompressFactor = 0.7f;
        }
        int remeasuredBitrate = (int) (originalBitrate / (Math.min(originalHeight / (float) (height), originalWidth / (float) (width))));
        remeasuredBitrate *= compressFactor;
        int minBitrate = (int) (getVideoBitrateWithFactor(minCompressFactor) / (1280f * 720f / (width * height)));
        if (originalBitrate < minBitrate) {
            return remeasuredBitrate;
        }
        if (remeasuredBitrate > maxBitrate) {
            return maxBitrate;
        }
        return Math.max(remeasuredBitrate, minBitrate);
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    private static int getVideoBitrateWithFactor(float f) {
        return (int) (f * 2000f * 1000f * 1.13f);
    }
}
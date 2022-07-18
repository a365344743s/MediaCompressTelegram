/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.media.MediaCodecInfo;
import android.os.Build;
import android.util.Log;

import org.telegram.ui.Components.AnimatedFileDrawable;

import chengdu.ws.telegram.BuildConfig;

public class Utilities {
    private static final String TAG = Utilities.class.getSimpleName();

    public static float clamp(float value, float maxValue, float minValue) {
        if (Float.isNaN(value)) {
            return minValue;
        }
        if (Float.isInfinite(value)) {
            return maxValue;
        }
        return Math.max(Math.min(value, maxValue), minValue);
    }

    public static VideoEditedInfo createCompressionSettings(String videoPath) {
        int[] params = new int[AnimatedFileDrawable.PARAM_NUM_COUNT];
        AnimatedFileDrawable.getVideoInfo(videoPath, params);

        if (params[AnimatedFileDrawable.PARAM_NUM_SUPPORTED_VIDEO_CODEC] == 0) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "video hasn't avc1 atom");
            }
            return null;
        }

        int originalBitrate = MediaController.getVideoBitrate(videoPath);
        if (originalBitrate == -1) {
            originalBitrate = params[AnimatedFileDrawable.PARAM_NUM_BITRATE];
        }
        int bitrate = originalBitrate;
        float videoDuration = params[AnimatedFileDrawable.PARAM_NUM_DURATION];
        int videoFramerate = params[AnimatedFileDrawable.PARAM_NUM_FRAMERATE];


        if (Build.VERSION.SDK_INT < 18) {
            try {
                MediaCodecInfo codecInfo = MediaController.selectCodec(MediaController.VIDEO_MIME_TYPE);
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
                        if (MediaController.selectColorFormat(codecInfo, MediaController.VIDEO_MIME_TYPE) == 0) {
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
        videoEditedInfo.bitrate = bitrate;
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
        bitrate = MediaController.makeVideoBitrate(
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
}

/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.graphics.Matrix;
import android.util.Log;

import org.telegram.messenger.video.MediaCodecVideoConvertor;

import java.io.File;
import java.util.ArrayList;

import chengdu.ws.common.FileUtils;
import chengdu.ws.telegram.BuildConfig;

public class MediaController {
    private static final String TAG = MediaController.class.getSimpleName();

    public final static String VIDEO_MIME_TYPE = "video/avc";
    public final static String AUIDO_MIME_TYPE = "audio/mp4a-latm";

    private final Object videoConvertSync = new Object();
    private final ArrayList<VideoConvertMessage> videoConvertQueue = new ArrayList<>();
    private int mNextConvertId = Integer.MIN_VALUE;

    private static volatile MediaController Instance;

    public static class CropState {
        public float cropPx;
        public float cropPy;
        public float cropScale = 1;
        public float cropRotate;
        public float cropPw = 1;
        public float cropPh = 1;
        public int transformWidth;
        public int transformHeight;
        public int transformRotation;
        public boolean mirrored;

        public float stateScale;
        public float scale;
        public Matrix matrix;
        public int width;
        public int height;
        public boolean freeform;
        public float lockedAspectRatio;

        public boolean initied;

        @Override
        public CropState clone() {
            CropState cloned = new CropState();

            cloned.cropPx = this.cropPx;
            cloned.cropPy = this.cropPy;
            cloned.cropScale = this.cropScale;
            cloned.cropRotate = this.cropRotate;
            cloned.cropPw = this.cropPw;
            cloned.cropPh = this.cropPh;
            cloned.transformWidth = this.transformWidth;
            cloned.transformHeight = this.transformHeight;
            cloned.transformRotation = this.transformRotation;
            cloned.mirrored = this.mirrored;

            cloned.stateScale = this.stateScale;
            cloned.scale = this.scale;
            cloned.matrix = this.matrix;
            cloned.width = this.width;
            cloned.height = this.height;
            cloned.freeform = this.freeform;
            cloned.lockedAspectRatio = this.lockedAspectRatio;

            cloned.initied = this.initied;
            return cloned;
        }
    }

    private static class VideoConvertMessage {
        public VideoEditedInfo videoEditedInfo;
        public ConvertorListener listener;

        public VideoConvertMessage(VideoEditedInfo info, ConvertorListener listener) {
            videoEditedInfo = info;
            this.listener = listener;
        }
    }

    public static MediaController getInstance() {
        MediaController localInstance = Instance;
        if (localInstance == null) {
            synchronized (MediaController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new MediaController();
                }
            }
        }
        return localInstance;
    }

    private MediaController() {
    }

    public void cleanup() {
        if (!videoConvertQueue.isEmpty()) {
            cancelVideoConvert(videoConvertQueue.get(0).videoEditedInfo);
        }
        videoConvertQueue.clear();
    }

    public boolean scheduleVideoConvert(VideoEditedInfo info, ConvertorListener listener) {
        return scheduleVideoConvert(info, listener, false);
    }

    public boolean scheduleVideoConvert(VideoEditedInfo info, ConvertorListener listener, boolean isEmpty) {
        if (info == null) {
            return false;
        }
        if (isEmpty && !videoConvertQueue.isEmpty()) {
            return false;
        } else if (isEmpty) {
            new File(info.attachPath).delete();
        }
        info.id = mNextConvertId;
        mNextConvertId++;
        videoConvertQueue.add(new VideoConvertMessage(info, listener));
        if (videoConvertQueue.size() == 1) {
            startVideoConvertFromQueue();
        }
        return true;
    }

    public void cancelVideoConvert(VideoEditedInfo info) {
        if (info != null) {
            if (!videoConvertQueue.isEmpty()) {
                for (int a = 0; a < videoConvertQueue.size(); a++) {
                    VideoConvertMessage videoConvertMessage = videoConvertQueue.get(a);
                    VideoEditedInfo object = videoConvertMessage.videoEditedInfo;
                    if (object.id == info.id) {
                        if (a == 0) {
                            synchronized (videoConvertSync) {
                                videoConvertMessage.videoEditedInfo.canceled = true;
                            }
                        } else {
                            videoConvertQueue.remove(a);
                        }
                        break;
                    }
                }
            }
        }
    }

    private boolean startVideoConvertFromQueue() {
        if (!videoConvertQueue.isEmpty()) {
            VideoConvertMessage videoConvertMessage = videoConvertQueue.get(0);
            VideoEditedInfo videoEditedInfo = videoConvertMessage.videoEditedInfo;
            synchronized (videoConvertSync) {
                if (videoEditedInfo != null) {
                    videoEditedInfo.canceled = false;
                }
            }
            VideoConvertRunnable.runConversion(videoConvertMessage);
            return true;
        }
        return false;
    }

    private void didWriteData(final VideoConvertMessage message, final File file, final boolean last, final long lastFrameTimestamp, long availableSize, final boolean error, final float progress) {
        final boolean firstWrite = message.videoEditedInfo.videoConvertFirstWrite;
        if (firstWrite) {
            message.videoEditedInfo.videoConvertFirstWrite = false;
        }
        VideoConvertUtil.getScheduler().runOnUIThread(() -> {
            if (error || last) {
                synchronized (videoConvertSync) {
                    message.videoEditedInfo.canceled = false;
                }
                videoConvertQueue.remove(message);
                startVideoConvertFromQueue();
            }
            if (error) {
                message.listener.onConvertFailed(message.videoEditedInfo, progress, lastFrameTimestamp);
            } else {
                if (firstWrite) {
                    message.listener.onConvertStart(message.videoEditedInfo, progress, lastFrameTimestamp);
                }
                if (last) {
                    message.listener.onConvertSuccess(message.videoEditedInfo, file.length(), lastFrameTimestamp);
                } else {
                    message.listener.onConvertProgress(message.videoEditedInfo, availableSize, progress, lastFrameTimestamp);
                }
            }
        });
    }

    private static class VideoConvertRunnable implements Runnable {

        private final VideoConvertMessage convertMessage;

        private VideoConvertRunnable(VideoConvertMessage message) {
            convertMessage = message;
        }

        @Override
        public void run() {
            MediaController.getInstance().convertVideo(convertMessage);
        }

        public static void runConversion(final VideoConvertMessage obj) {
            VideoConvertUtil.getScheduler().runOnComputationThread(() -> {
                try {
                    new VideoConvertRunnable(obj).run();
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    private boolean convertVideo(final VideoConvertMessage convertMessage) {
        VideoEditedInfo info = convertMessage.videoEditedInfo;
        if (info == null) {
            return false;
        }
        String videoPath = info.originalPath;
        long startTime = info.startTime;
        long avatarStartTime = info.avatarStartTime;
        long endTime = info.endTime;
        int resultWidth = info.resultWidth;
        int resultHeight = info.resultHeight;
        int rotationValue = info.rotationValue;
        int originalWidth = info.originalWidth;
        int originalHeight = info.originalHeight;
        int framerate = info.framerate;
        int bitrate = info.bitrate;
        int originalBitrate = info.originalBitrate;
//        boolean isSecret = DialogObject.isEncryptedDialog(messageObject.getDialogId());
        boolean isSecret = false;
        final File cacheFile = new File(info.attachPath);
        FileUtils.createFileByDeleteOldFile(info.attachPath);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "begin convert " + videoPath + " startTime = " + startTime + " avatarStartTime = " + avatarStartTime + " endTime " + endTime + " rWidth = " + resultWidth + " rHeight = " + resultHeight + " rotation = " + rotationValue + " oWidth = " + originalWidth + " oHeight = " + originalHeight + " framerate = " + framerate + " bitrate = " + bitrate + " originalBitrate = " + originalBitrate);
        }

        if (videoPath == null) {
            videoPath = "";
        }

        long duration;
        if (startTime > 0 && endTime > 0) {
            duration = endTime - startTime;
        } else if (endTime > 0) {
            duration = endTime;
        } else if (startTime > 0) {
            duration = info.originalDuration - startTime;
        } else {
            duration = info.originalDuration;
        }

        if (framerate == 0) {
            framerate = 25;
        } else if (framerate > 59) {
            framerate = 59;
        }

        if (rotationValue == 90 || rotationValue == 270) {
            int temp = resultHeight;
            resultHeight = resultWidth;
            resultWidth = temp;
        }

        if (!info.shouldLimitFps && framerate > 40 && (Math.min(resultHeight, resultWidth) <= 480)) {
            framerate = 30;
        }

        boolean needCompress = avatarStartTime != -1 || info.cropState != null ||
                resultWidth != originalWidth || resultHeight != originalHeight || rotationValue != 0 || info.roundVideo || startTime != -1;


//        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("videoconvert", Activity.MODE_PRIVATE);

        long time = System.currentTimeMillis();

        VideoConvertorListener callback = new VideoConvertorListener() {

            private long lastAvailableSize = 0;

            @Override
            public boolean checkConversionCanceled() {
                return info.canceled;
            }

            @Override
            public void didWriteData(long availableSize, float progress) {
                if (info.canceled) {
                    return;
                }
                if (availableSize < 0) {
                    availableSize = cacheFile.length();
                }

                if (!info.needUpdateProgress && lastAvailableSize == availableSize) {
                    return;
                }

                lastAvailableSize = availableSize;
                MediaController.this.didWriteData(convertMessage, cacheFile, false, 0, availableSize, false, progress);
            }
        };

        info.videoConvertFirstWrite = true;

        MediaCodecVideoConvertor videoConvertor = new MediaCodecVideoConvertor();
        boolean error = videoConvertor.convertVideo(videoPath, cacheFile,
                rotationValue, isSecret,
                originalWidth, originalHeight,
                resultWidth, resultHeight,
                framerate, bitrate, originalBitrate,
                startTime, endTime, avatarStartTime,
                needCompress, duration,
                info.cropState,
                info.roundVideo,
                callback);


        boolean canceled = info.canceled;
        if (!canceled) {
            synchronized (videoConvertSync) {
                canceled = info.canceled;
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "time=" + (System.currentTimeMillis() - time) + " canceled=" + canceled);
        }

//        preferences.edit().putBoolean("isPreviousOk", true).apply();
        didWriteData(convertMessage, cacheFile, true, videoConvertor.getLastFrameTimestamp(), cacheFile.length(), error || canceled, 1f);

        return true;
    }

    public interface VideoConvertorListener {
        boolean checkConversionCanceled();

        void didWriteData(long availableSize, float progress);
    }

    public interface ConvertorListener {
        /**
         * 转换开始
         */
        void onConvertStart(VideoEditedInfo info, float progress, long lastFrameTimestamp);

        /**
         * 转换进度
         */
        void onConvertProgress(VideoEditedInfo info, long availableSize, float progress, long lastFrameTimestamp);

        /**
         * 转换成功
         */
        void onConvertSuccess(VideoEditedInfo info, long fileLength, long lastFrameTimestamp);

        /**
         * 转换失败
         */
        void onConvertFailed(VideoEditedInfo info, float progress, long lastFrameTimestamp);
    }
}

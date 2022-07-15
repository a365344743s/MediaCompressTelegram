/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import org.telegram.messenger.video.MediaCodecVideoConvertor;
import org.telegram.ui.Components.PhotoFilterView;

import java.io.File;
import java.util.ArrayList;

import chengdu.ws.telegram.BuildConfig;

public class MediaController {
    private static final String TAG = MediaController.class.getSimpleName();

    public static class SavedFilterState {
        public float enhanceValue;
        public float softenSkinValue;
        public float exposureValue;
        public float contrastValue;
        public float warmthValue;
        public float saturationValue;
        public float fadeValue;
        public int tintShadowsColor;
        public int tintHighlightsColor;
        public float highlightsValue;
        public float shadowsValue;
        public float vignetteValue;
        public float grainValue;
        public int blurType;
        public float sharpenValue;
        public PhotoFilterView.CurvesToolValue curvesToolValue = new PhotoFilterView.CurvesToolValue();
        public float blurExcludeSize;
        public org.telegram.ui.Components.Point blurExcludePoint;
        public float blurExcludeBlurSize;
        public float blurAngle;
    }

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

    public final static String VIDEO_MIME_TYPE = "video/avc";
    public final static String AUIDO_MIME_TYPE = "audio/mp4a-latm";

    private final Object videoConvertSync = new Object();

    private static class VideoConvertMessage {
        public MessageObject messageObject;
        public VideoEditedInfo videoEditedInfo;
        public int currentAccount;

        public VideoConvertMessage(MessageObject object, VideoEditedInfo info) {
            messageObject = object;
            currentAccount = messageObject.currentAccount;
            videoEditedInfo = info;
        }
    }

    private ArrayList<VideoConvertMessage> videoConvertQueue = new ArrayList<>();

    private static volatile MediaController Instance;

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

    public MediaController() {
    }

    public void cleanup() {
        videoConvertQueue.clear();
        cancelVideoConvert(null);
    }

    public void scheduleVideoConvert(MessageObject messageObject) {
        scheduleVideoConvert(messageObject, false);
    }

    public boolean scheduleVideoConvert(MessageObject messageObject, boolean isEmpty) {
        if (messageObject == null || messageObject.videoEditedInfo == null) {
            return false;
        }
        if (isEmpty && !videoConvertQueue.isEmpty()) {
            return false;
        } else if (isEmpty) {
            new File(messageObject.messageOwner.attachPath).delete();
        }
        videoConvertQueue.add(new VideoConvertMessage(messageObject, messageObject.videoEditedInfo));
        if (videoConvertQueue.size() == 1) {
            startVideoConvertFromQueue();
        }
        return true;
    }

    public void cancelVideoConvert(MessageObject messageObject) {
        if (messageObject != null) {
            if (!videoConvertQueue.isEmpty()) {
                for (int a = 0; a < videoConvertQueue.size(); a++) {
                    VideoConvertMessage videoConvertMessage = videoConvertQueue.get(a);
                    MessageObject object = videoConvertMessage.messageObject;
                    if (object.equals(messageObject) && object.currentAccount == messageObject.currentAccount) {
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

    @SuppressLint("NewApi")
    public static MediaCodecInfo selectCodec(String mimeType) {
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

    @SuppressLint("NewApi")
    public static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
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

    public static int findTrack(MediaExtractor extractor, boolean audio) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.startsWith("audio/")) {
                    return i;
                }
            } else {
                if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return -5;
    }

    private void didWriteData(final VideoConvertMessage message, final File file, final boolean last, final long lastFrameTimestamp, long availableSize, final boolean error, final float progress) {
        final boolean firstWrite = message.videoEditedInfo.videoConvertFirstWrite;
        if (firstWrite) {
            message.videoEditedInfo.videoConvertFirstWrite = false;
        }
        AndroidUtilities.runOnUIThread(() -> {
            if (error || last) {
                synchronized (videoConvertSync) {
                    message.videoEditedInfo.canceled = false;
                }
                videoConvertQueue.remove(message);
                startVideoConvertFromQueue();
            }
            if (error) {
                NotificationCenter.getInstance(message.currentAccount).postNotificationName(NotificationCenter.filePreparingFailed, message.messageObject, file.toString(), progress, lastFrameTimestamp);
            } else {
                if (firstWrite) {
                    NotificationCenter.getInstance(message.currentAccount).postNotificationName(NotificationCenter.filePreparingStarted, message.messageObject, file.toString(), progress, lastFrameTimestamp);
                }
                NotificationCenter.getInstance(message.currentAccount).postNotificationName(NotificationCenter.fileNewChunkAvailable, message.messageObject, file.toString(), availableSize, last ? file.length() : 0, progress, lastFrameTimestamp);
            }
        });
    }

    private static class VideoConvertRunnable implements Runnable {

        private VideoConvertMessage convertMessage;

        private VideoConvertRunnable(VideoConvertMessage message) {
            convertMessage = message;
        }

        @Override
        public void run() {
            MediaController.getInstance().convertVideo(convertMessage);
        }

        public static void runConversion(final VideoConvertMessage obj) {
            new Thread(() -> {
                try {
                    VideoConvertRunnable wrapper = new VideoConvertRunnable(obj);
                    Thread th = new Thread(wrapper, "VideoConvertRunnable");
                    th.start();
                    th.join();
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }).start();
        }
    }


    private boolean convertVideo(final VideoConvertMessage convertMessage) {
        MessageObject messageObject = convertMessage.messageObject;
        VideoEditedInfo info = convertMessage.videoEditedInfo;
        if (messageObject == null || info == null) {
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
        boolean isSecret = DialogObject.isEncryptedDialog(messageObject.getDialogId());
        final File cacheFile = new File(messageObject.messageOwner.attachPath);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
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

        boolean needCompress = avatarStartTime != -1 || info.cropState != null || info.mediaEntities != null || info.paintPath != null || info.filterState != null ||
                resultWidth != originalWidth || resultHeight != originalHeight || rotationValue != 0 || info.roundVideo || startTime != -1;


        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("videoconvert", Activity.MODE_PRIVATE);

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
                info.filterState,
                info.paintPath,
                info.mediaEntities,
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

        preferences.edit().putBoolean("isPreviousOk", true).apply();
        didWriteData(convertMessage, cacheFile, true, videoConvertor.getLastFrameTimestamp(), cacheFile.length(), error || canceled, 1f);

        return true;
    }

    public interface VideoConvertorListener {
        boolean checkConversionCanceled();

        void didWriteData(long availableSize, float progress);
    }
}

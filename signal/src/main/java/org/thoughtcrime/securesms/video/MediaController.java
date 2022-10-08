package org.thoughtcrime.securesms.video;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.thoughtcrime.securesms.mms.MediaStream;
import org.thoughtcrime.securesms.util.FileMediaDataSource;
import org.thoughtcrime.securesms.util.FileUtils;
import org.thoughtcrime.securesms.util.MemoryFileDescriptor;
import org.thoughtcrime.securesms.util.VideoConvertUtil;
import org.thoughtcrime.securesms.video.videoconverter.EncodingException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class MediaController {
    private static final String TAG = MediaController.class.getSimpleName();
    private static volatile MediaController sInstance;

    private final ArrayList<Task> videoConvertQueue = new ArrayList<>();
    private int mNextConvertId = Integer.MIN_VALUE;

    public static MediaController getInstance() {
        if (sInstance == null) {
            synchronized (MediaController.class) {
                if (sInstance == null) {
                    sInstance = new MediaController();
                }
            }
        }
        return sInstance;
    }

    private MediaController() {
    }

    public void cleanup() {
        if (!videoConvertQueue.isEmpty()) {
            cancelVideoConvert(videoConvertQueue.get(0).id);
        }
        videoConvertQueue.clear();
    }

    @RequiresApi(26)
    @Nullable
    public Integer scheduleVideoConvert(String videoPath, String attachPath, long upperSizeLimit, boolean useMemory, ConvertorListener listener) {
        if (Build.VERSION.SDK_INT < 26 || (useMemory && !MemoryFileDescriptor.supported())) {
            return null;
        }
        Task task = new Task(videoPath, attachPath, upperSizeLimit, useMemory, listener, mNextConvertId);
        mNextConvertId++;
        videoConvertQueue.add(task);
        if (videoConvertQueue.size() == 1) {
            startVideoConvertFromQueue();
        }
        return task.id;
    }

    public void cancelVideoConvert(int id) {
        if (!videoConvertQueue.isEmpty()) {
            for (int a = 0; a < videoConvertQueue.size(); a++) {
                Task task = videoConvertQueue.get(a);
                if (task.id == id) {
                    if (a == 0) {
                        task.canceled = true;
                    } else {
                        videoConvertQueue.remove(a);
                    }
                    break;
                }
            }
        }
    }

    @RequiresApi(26)
    private boolean startVideoConvertFromQueue() {
        if (!videoConvertQueue.isEmpty()) {
            Task task = videoConvertQueue.get(0);
            task.canceled = false;
            VideoConvertUtil.getScheduler().runOnComputationThread(() -> {
                if (task.useMemory) {
                    try (InMemoryTranscoder transcoder = new InMemoryTranscoder(VideoConvertUtil.getApp(), new File(task.videoPath), null, task.upperSizeLimit)) {
                        final File cacheFile = new File(task.attachPath);
                        if (cacheFile.exists()) {
                            if (!cacheFile.delete()) {
                                throw new IOException("Delete file failed: " + task.attachPath);
                            }
                        }
                        if (transcoder.isTranscodeRequired()) {
                            try (MediaStream ms = transcoder.transcode(percent -> MediaController.this.onProgress(task, percent), () -> task.canceled)) {
                                if (!chengdu.ws.common.FileUtils.createFileByDeleteOldFile(task.attachPath)) {
                                    throw new IOException("createFileByDeleteOldFile failed: " + task.attachPath);
                                }
                                if (!chengdu.ws.common.FileUtils.writeFileFromIS(task.attachPath, ms.getStream())) {
                                    throw new IOException("WriteFileFromIS failed: " + task.attachPath);
                                }
                                MediaController.this.onFinish(task, null);
                            }
                        } else {
                            chengdu.ws.common.FileUtils.copyFile(task.videoPath, task.attachPath);
                            MediaController.this.onFinish(task, null);
                        }
                    } catch (IOException | VideoSourceException | EncodingException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                        e.printStackTrace();
                        MediaController.this.onFinish(task, e);
                    }
                } else {
                    try {
                        StreamingTranscoder transcoder = new StreamingTranscoder(new File(task.videoPath), null, task.upperSizeLimit);
                        if (!chengdu.ws.common.FileUtils.createFileByDeleteOldFile(task.attachPath)) {
                            throw new IOException("createFileByDeleteOldFile failed: " + task.attachPath);
                        }
                        if (transcoder.isTranscodeRequired()) {
                            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(task.attachPath))) {
                                transcoder.transcode(percent -> MediaController.this.onProgress(task, percent), os, () -> task.canceled);
                                MediaController.this.onFinish(task, null);
                            }
                        } else {
                            chengdu.ws.common.FileUtils.copyFile(task.videoPath, task.attachPath);
                            MediaController.this.onFinish(task, null);
                        }
                    } catch (IOException | VideoSourceException | EncodingException e) {
                        Log.e(TAG, e.getLocalizedMessage());
                        e.printStackTrace();
                        MediaController.this.onFinish(task, e);
                    }
                }
            });
            return true;
        }
        return false;
    }

    private void onProgress(Task task, int percent) {
        VideoConvertUtil.getScheduler().runOnUIThread(() -> task.listener.onConvertProgress(task, percent / 100.f));
    }

    @RequiresApi(26)
    private void onFinish(Task task, Exception error) {
        VideoConvertUtil.getScheduler().runOnUIThread(() -> {
            videoConvertQueue.remove(task);
            startVideoConvertFromQueue();
            if (error != null || task.canceled) {
                task.listener.onConvertFailed(task);
            } else {
                task.listener.onConvertSuccess(task);
            }
        });
    }

    public static class Task {
        private final String videoPath;
        private final String attachPath;
        private final long upperSizeLimit;
        private final boolean useMemory;
        private final ConvertorListener listener;
        private final int id;
        private boolean canceled;

        private Task(String videoPath, String attachPath, long upperSizeLimit, boolean useMemory, ConvertorListener listener, int id) {
            this.videoPath = videoPath;
            this.attachPath = attachPath;
            this.upperSizeLimit = upperSizeLimit;
            this.useMemory = useMemory;
            this.listener = listener;
            this.id = id;
        }

        public String getVideoPath() {
            return videoPath;
        }

        public String getAttachPath() {
            return attachPath;
        }

        public long getUpperSizeLimit() {
            return upperSizeLimit;
        }

        public boolean isUseMemory() {
            return useMemory;
        }

        public ConvertorListener getListener() {
            return listener;
        }

        public int getId() {
            return id;
        }

        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public String toString() {
            return "Task{" +
                    "videoPath='" + videoPath + '\'' +
                    ", attachPath='" + attachPath + '\'' +
                    ", upperSizeLimit=" + upperSizeLimit +
                    ", useMemory=" + useMemory +
                    ", listener=" + listener +
                    ", id=" + id +
                    ", canceled=" + canceled +
                    '}';
        }
    }

    public interface ConvertorListener {
        /**
         * 转换进度
         */
        void onConvertProgress(Task task, float progress);

        /**
         * 转换成功
         */
        void onConvertSuccess(Task task);

        /**
         * 转换失败
         */
        void onConvertFailed(Task task);
    }
}

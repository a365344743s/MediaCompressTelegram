package org.thoughtcrime.securesms.util;

import android.app.Application;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.thoughtcrime.securesms.video.MediaController;

public class VideoConvertUtil {
    private static Application sApp;
    private static Scheduler sScheduler;

    /**
     * 初始化
     */
    public static void init(Application application, Scheduler scheduler) {
        sApp = application;
        sScheduler = scheduler;
    }

    public static Application getApp() {
        return sApp;
    }

    public static Scheduler getScheduler() {
        return sScheduler;
    }

    /**
     * 开始视频转换
     *
     * @param videoPath      视频路径
     * @param attachPath     转换后路径
     * @param upperSizeLimit 最大尺寸限制
     * @param useMemory      是否使用MemoryFile
     * @param listener       回调
     * @return 唯一id
     */
    @RequiresApi(26)
    @Nullable
    public static Integer startVideoConvert(String videoPath, String attachPath, long upperSizeLimit, boolean useMemory, MediaController.ConvertorListener listener) {
        return MediaController.getInstance().scheduleVideoConvert(videoPath, attachPath, upperSizeLimit, useMemory, listener);
    }

    /**
     * 停止视频转换
     *
     * @param id 唯一id
     */
    public static void stopVideoConvert(int id) {
        MediaController.getInstance().cancelVideoConvert(id);
    }
}

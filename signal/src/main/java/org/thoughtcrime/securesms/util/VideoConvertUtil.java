package org.thoughtcrime.securesms.util;

import android.app.Application;

import androidx.annotation.Nullable;

import org.thoughtcrime.securesms.video.MediaController;

import chengdu.ws.common.Scheduler;

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
     * @param listener       回调
     * @return 唯一id
     */
    @Nullable
    public static Integer startVideoConvert(String videoPath, String attachPath, long upperSizeLimit, MediaController.ConvertorListener listener) {
        return MediaController.getInstance().scheduleVideoConvert(videoPath, attachPath, upperSizeLimit, false, listener);
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

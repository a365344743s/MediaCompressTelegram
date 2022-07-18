/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

public class VideoEditedInfo {
    public int originalWidth;
    public int originalHeight;
    public int originalBitrate;
    public long originalDuration;
    public long startTime;
    public long endTime;
    public String originalPath;
    public String attachPath;
    public int resultWidth;
    public int resultHeight;

    public long avatarStartTime = -1;
    public int rotationValue;
    public int bitrate;
    public int framerate = 24;
    public boolean roundVideo;
    public MediaController.CropState cropState;

    public int id = Integer.MAX_VALUE;
    public boolean canceled;
    public boolean videoConvertFirstWrite;
    public boolean needUpdateProgress = false;
    public boolean shouldLimitFps = true;
}

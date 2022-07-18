/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

public class VideoEditedInfo {

    public long startTime;
    public long endTime;
    public long avatarStartTime = -1;
    public int rotationValue;
    public int originalWidth;
    public int originalHeight;
    public int originalBitrate;
    public int resultWidth;
    public int resultHeight;
    public int bitrate;
    public int framerate = 24;
    public String originalPath;
    public String attachPath;
    public boolean roundVideo;
    public long originalDuration;
    public MediaController.CropState cropState;

    public int id = Integer.MAX_VALUE;
    public boolean canceled;
    public boolean videoConvertFirstWrite;
    public boolean needUpdateProgress = false;
    public boolean shouldLimitFps = true;
}

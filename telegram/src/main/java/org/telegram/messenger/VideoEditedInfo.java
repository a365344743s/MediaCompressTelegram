/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import java.util.ArrayList;

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
    public boolean roundVideo;
    public long originalDuration;
    public MediaController.SavedFilterState filterState;
    public String paintPath;
    public ArrayList<MediaEntity> mediaEntities;
    public MediaController.CropState cropState;

    public boolean canceled;
    public boolean videoConvertFirstWrite;
    public boolean needUpdateProgress = false;
    public boolean shouldLimitFps = true;

    public static class MediaEntity {
        public byte type;
        public byte subType;
        public float x;
        public float y;
        public float rotation;
        public float width;
        public float height;
        public String text;
        public int color;
        public int fontSize;
        public int viewWidth;
        public int viewHeight;

        public float scale;
        public float textViewWidth;
        public float textViewHeight;
        public float textViewX;
        public float textViewY;

        public MediaEntity() {

        }

        public MediaEntity copy() {
            MediaEntity entity = new MediaEntity();
            entity.type = type;
            entity.subType = subType;
            entity.x = x;
            entity.y = y;
            entity.rotation = rotation;
            entity.width = width;
            entity.height = height;
            entity.text = text;
            entity.color = color;
            entity.fontSize = fontSize;
            entity.viewWidth = viewWidth;
            entity.viewHeight = viewHeight;
            entity.scale = scale;
            entity.textViewWidth = textViewWidth;
            entity.textViewHeight = textViewHeight;
            entity.textViewX = textViewX;
            entity.textViewY = textViewY;
            return entity;
        }
    }
}

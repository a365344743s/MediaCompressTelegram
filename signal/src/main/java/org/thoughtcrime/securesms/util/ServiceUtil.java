package org.thoughtcrime.securesms.util;

import android.app.ActivityManager;
import android.content.Context;

import androidx.annotation.NonNull;

public class ServiceUtil {
    public static ActivityManager getActivityManager(@NonNull Context context) {
        return (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }
}

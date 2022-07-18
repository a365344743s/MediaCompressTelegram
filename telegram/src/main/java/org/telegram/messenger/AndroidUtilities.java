/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Application;
import android.os.Handler;


public class AndroidUtilities {
    private static Handler sApplicationHandler;

    public static void init(Application application) {
        sApplicationHandler = new android.os.Handler(application.getApplicationContext().getMainLooper());
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (sApplicationHandler == null) {
            return;
        }
        if (delay == 0) {
            sApplicationHandler.post(runnable);
        } else {
            sApplicationHandler.postDelayed(runnable, delay);
        }
    }

    public static void cancelRunOnUIThread(Runnable runnable) {
        if (sApplicationHandler == null) {
            return;
        }
        sApplicationHandler.removeCallbacks(runnable);
    }
}

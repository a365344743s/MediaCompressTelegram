package org.thoughtcrime.securesms.util;

public interface Scheduler {

    void runOnComputationThread(Runnable runnable);

    void runOnUIThread(Runnable runnable);
}

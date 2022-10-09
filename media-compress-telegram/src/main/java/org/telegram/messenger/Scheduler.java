package org.telegram.messenger;

public interface Scheduler {

    void runOnComputationThread(Runnable runnable);

    void runOnUIThread(Runnable runnable);
}

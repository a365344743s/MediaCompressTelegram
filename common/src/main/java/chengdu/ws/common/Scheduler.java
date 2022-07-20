package chengdu.ws.common;

public interface Scheduler {

    void runOnComputationThread(Runnable runnable);

    void runOnUIThread(Runnable runnable);
}

package chengdu.ws.mediacompress;

import android.util.Log;

import chengdu.ws.common.Scheduler;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Application extends android.app.Application {
    private static final String TAG = Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        RxJavaPlugins.setErrorHandler(throwable -> {
            Log.e(TAG, throwable.getLocalizedMessage());
            throwable.printStackTrace();
        });
        Scheduler scheduler = new Scheduler() {
            @Override
            public void runOnComputationThread(Runnable runnable) {
                Schedulers.computation().scheduleDirect(runnable);
            }

            @Override
            public void runOnUIThread(Runnable runnable) {
                AndroidSchedulers.mainThread().scheduleDirect(runnable);
            }
        };
        org.thoughtcrime.securesms.util.VideoConvertUtil.init(this, scheduler);
        org.telegram.messenger.VideoConvertUtil.init(scheduler);
    }
}

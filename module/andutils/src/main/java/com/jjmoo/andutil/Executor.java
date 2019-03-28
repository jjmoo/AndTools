package com.jjmoo.andutil;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * @author Zohn
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Executor {
    private static final String TAG = "Executor";
    private static final String BG_THREAD_NAME = "executor-bg-thread";

    private static HandlerThread sBgThread;
    private static Handler sBgHandler;
    private static Handler sMainHandler;

    static {
        sBgThread = new HandlerThread(BG_THREAD_NAME);
        sBgThread.start();
        sBgHandler = new Handler(sBgThread.getLooper());
        sMainHandler = new Handler(Looper.getMainLooper());
    }

    public static void runOnBgThread(Runnable runnable) {
        sBgHandler.post(new Command(runnable));
    }

    public static void runOnBgThreadDelayed(Runnable runnable, int millisecond) {
        sBgHandler.postDelayed(new Command(runnable), millisecond);
    }

    public static void runOnUiThread(Runnable runnable) {
        sMainHandler.post(new Command(runnable));
    }

    public static void runOnUiThreadDelayed(Runnable runnable, int millisecond) {
        sMainHandler.postDelayed(new Command(runnable), millisecond);
    }

    public static Looper getBgLooper() {
        return sBgThread.getLooper();
    }

    private static class Command implements Runnable {
        Runnable mRunnable;
        RuntimeException mStack;

        Command(Runnable runnable) {
            mRunnable = runnable;
            mStack = new RuntimeException();
        }

        @Override
        public void run() {
            try {
                mRunnable.run();
            } catch (RuntimeException e) {
                mStack.initCause(e);
                JmLog.w(TAG, "run # failed to execute runnable: " + mRunnable, mStack);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> {
                        throw mStack;
                    });
                }
            }
        }
    }
}

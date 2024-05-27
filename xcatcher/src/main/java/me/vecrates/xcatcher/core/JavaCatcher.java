package me.vecrates.xcatcher.core;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class JavaCatcher {

    private static final String TAG = "JavaCatcher";

    private static OnCrashListener onCrashListener;

    private JavaCatcher() {

    }

    public static void init() {
        listenMainThread();
        listenOtherThread();
    }

    private static void listenMainThread() {
        new Handler(Looper.getMainLooper()).post(() -> {
            while (true) {
                try {
                    Looper.loop();
                } catch (Throwable e) {
                    Log.e(TAG, "main thread:\n", e);
                    if (onCrash(e)) {
                        exitProcess();
                    }
                }
            }
        });
    }

    private static void listenOtherThread() {
        //Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.e(TAG, "other thread:\n", e);
            if (onCrash(e)) {
                exitProcess();
            }
        });
    }

    private static boolean onCrash(Throwable throwable) {
        return onCrashListener != null && onCrashListener.onCrash(throwable);
    }

    private static void exitProcess() {
        System.exit(0);
    }

    public static void setOnCrashListener(OnCrashListener onCrashListener) {
        JavaCatcher.onCrashListener = onCrashListener;
    }

}

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
                    int handle = notifyListener(e);
                    if (handle == CrashHandle.THROW) {
                        throw e;
                    }
                    if (handle == CrashHandle.EXIT) {
                        Log.e(TAG, "main thread:\n", e);
                        exitProcess();
                        return;
                    }
                    Log.e(TAG, "main thread:\n", e);
                }
            }
        });
    }

    private static void listenOtherThread() {
        Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            int handle = notifyListener(e);
            if (handle == CrashHandle.THROW) {
                if (oldHandler != null) {
                    oldHandler.uncaughtException(t, e);
                } else {
                    throw new RuntimeException(e);
                }
                return;
            }
            if (handle == CrashHandle.EXIT) {
                Log.e(TAG, "other thread:\n", e);
                exitProcess();
                return;
            }
            Log.e(TAG, "other thread:\n", e);
        });
    }

    private static int notifyListener(Throwable throwable) {
        if (onCrashListener == null) {
            return CrashHandle.THROW;
        }
        return onCrashListener.onCrash(throwable);
    }

    private static void exitProcess() {
        System.exit(0);
    }

    public static void setOnCrashListener(OnCrashListener onCrashListener) {
        JavaCatcher.onCrashListener = onCrashListener;
    }

}

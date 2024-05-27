package me.vecrates.xcatcher.core;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.PrintStream;
import java.util.Set;

public class NativeCatcher {

    private static final String TAG = "NativeCatcher";

    static {
        System.loadLibrary("catcher");
    }

    private OnCrashListener nativeCrashListener;

    private static class H {
        private static final NativeCatcher INSTANCE = new NativeCatcher();
    }

    public static NativeCatcher getInstance() {
        return H.INSTANCE;
    }

    private NativeCatcher() {
    }

    /**
     * native 层产生崩溃时的回调
     */
    public void onCrash(NativeCrash nativeCrash) {

        NativeThrowable e;
        String crashThreadName = "<unknown>";

        if (nativeCrash != null) {
            e = new NativeThrowable(nativeCrash.stackTraces,
                    getStackTracesByThread(nativeCrash.threadName));
            crashThreadName = nativeCrash.threadName;
        } else {
            e = new NativeThrowable("");
        }

        Log.e(TAG, "onCrash: "
                + "\n#####################"
                + "\nhandle thread=" + Thread.currentThread().getName()
                + "\ncrash thread=" + crashThreadName
                + "\n#####################", e);

        if (nativeCrashListener != null) {
            nativeCrashListener.onCrash(e);
        }

    }

    /**
     * 根据线程名获得线程对象，native层会调用该方法，不能被混淆
     */
    private Thread getThreadByName(String threadName) {
        if (TextUtils.isEmpty(threadName)) {
            return Looper.getMainLooper().getThread();
        }

        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[0]);

        Thread theThread = null;
        for (Thread thread : threadArray) {
            if (threadName.equals(thread.getName())) {
                theThread = thread;
            }
        }

        //没有线程名的视为主线程
        return theThread != null ? theThread : Looper.getMainLooper().getThread();
    }

    private void printStackTrace(String threadName) {
        Thread thread = getThreadByName(threadName);
        PrintStream s = System.err;
        StackTraceElement[] trace = thread.getStackTrace();
        s.println(NativeCatcher.class);
        for (StackTraceElement traceElement : trace) {
            s.println("\tat " + traceElement);
        }
    }

    private StackTraceElement[] getStackTracesByThread(String threadName) {
        Thread thread = getThreadByName(threadName);
        return thread.getStackTrace();
    }

    public void setNativeCrashListener(OnCrashListener nativeCrashListener) {
        this.nativeCrashListener = nativeCrashListener;
    }

    public native void init();

    public native void crash();

}

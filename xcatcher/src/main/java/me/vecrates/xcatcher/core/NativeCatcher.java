package me.vecrates.xcatcher.core;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.PrintStream;
import java.util.Set;

public class NativeCatcher {

    private static final String TAG = "NativeCatcher";

    static {
        System.loadLibrary("xcatcher");
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
     * @return 是否退出进程，如果返回 false，崩溃 signal 将交给其它的 signal 处理函数处理
     */
    public boolean onCrash(NativeCrash nativeCrash) {

        NativeThrowable e = generateNativeThrowable(nativeCrash);
        String crashThreadName = nativeCrash != null ? nativeCrash.threadName : "<unknown>";

        int handle = CrashHandle.THROW;
        if (nativeCrashListener != null) {
            handle = nativeCrashListener.onCrash(e);
        }

        Log.e(TAG, "onCrash: "
                + "\n#####################"
                + "\nhandle thread=" + Thread.currentThread().getName()
                + "\ncrash thread=" + crashThreadName
                + "\n#####################", e);

        return handle == CrashHandle.EXIT;
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

    @NonNull
    private NativeThrowable generateNativeThrowable(NativeCrash nativeCrash) {
        if (nativeCrash == null) {
            return new NativeThrowable("");
        }

        StackTraceElement[] jvmElements = getJvmStackTracesByThread(nativeCrash.threadName);
        StackTraceElement[] nativeElements = covertNativeStackElements(nativeCrash.stackTraces);
        if (nativeElements == null) {
            return new NativeThrowable(nativeCrash.message, jvmElements);
        }

        StackTraceElement[] allElements = new StackTraceElement[jvmElements.length + nativeElements.length];
        System.arraycopy(nativeElements, 0, allElements, 0, nativeElements.length);
        System.arraycopy(jvmElements, 0, allElements, nativeElements.length, jvmElements.length);

        return new NativeThrowable(nativeCrash.message, allElements);
    }

    @Nullable
    private StackTraceElement[] covertNativeStackElements(String nativeStacktrace) {
        if (TextUtils.isEmpty(nativeStacktrace)) {
            return null;
        }
        String[] traces = nativeStacktrace.split("\n");
        StackTraceElement[] elements = new StackTraceElement[traces.length];
        for (int i = 0; i < traces.length; i++) {
            elements[i] = new StackTraceElement(traces[i], "", "", -2);
        }
        return elements;
    }

    @NonNull
    private StackTraceElement[] getJvmStackTracesByThread(String threadName) {
        Thread thread = getThreadByName(threadName);
        return thread.getStackTrace();
    }

    /**
     * 根据线程名获得线程对象
     */
    @NonNull
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


    public void setNativeCrashListener(OnCrashListener nativeCrashListener) {
        this.nativeCrashListener = nativeCrashListener;
    }

    public native void init();

    public native void crash();

}

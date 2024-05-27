package me.vecrates.xcatcher;

import androidx.annotation.Nullable;

import me.vecrates.xcatcher.core.JavaCatcher;
import me.vecrates.xcatcher.core.NativeCatcher;
import me.vecrates.xcatcher.core.OnCrashListener;

public class Catcher {

    private static CrashCollector crashCollector;

    private Catcher() {

    }

    /**
     * @param collector 当崩溃发生时，collector 接收到崩溃
     */
    public static void init(@Nullable CrashCollector collector) {
        crashCollector = collector;

        JavaCatcher.init();
        NativeCatcher.getInstance().init();

        JavaCatcher.setOnCrashListener(ON_CRASH_LISTENER);
        NativeCatcher.getInstance().setNativeCrashListener(ON_CRASH_LISTENER);
    }

    /**
     * 主动触发 native 崩溃
     */
    public static void triggerNativeCash() {
        NativeCatcher.getInstance().crash();
    }

    private static final OnCrashListener ON_CRASH_LISTENER = e -> crashCollector == null || crashCollector.doCollect(e);

}

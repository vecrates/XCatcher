package me.vecrates.xcatcher;

import android.util.Log;

import androidx.annotation.NonNull;

import me.vecrates.xcatcher.core.CrashHandle;
import me.vecrates.xcatcher.core.JavaCatcher;
import me.vecrates.xcatcher.core.LogcatReader;
import me.vecrates.xcatcher.core.NativeCatcher;
import me.vecrates.xcatcher.core.OnCrashListener;

public class XCatcher {

    private static InitConfig config;

    private XCatcher() {

    }

    /**
     * @param config 是否需要捕获 native 崩溃
     */
    public static void init(@NonNull InitConfig config) {
        XCatcher.config = config;

        JavaCatcher.init();
        JavaCatcher.setOnCrashListener(LISTENER);

        if (config.catchNative) {
            NativeCatcher.getInstance().init();
            NativeCatcher.getInstance().setNativeCrashListener(LISTENER);
        }
    }

    /**
     * 主动触发 native 崩溃
     */
    public static void triggerNativeCash() {
        NativeCatcher.getInstance().crash();
    }

    private static final OnCrashListener LISTENER = e -> {
        if (config == null || config.collector == null) {
            return CrashHandle.THROW;
        }
        return config.collector.doCollect(e, LogcatReader.readLogcat(config.readLogcatLines));
    };

}

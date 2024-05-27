package me.vecrates.xcatcher.core;

import androidx.annotation.NonNull;

public interface OnCrashListener {

    /**
     * 发生异常时触发
     *
     * @return 是否退出进程
     */
    boolean onCrash(@NonNull Throwable throwable);
}

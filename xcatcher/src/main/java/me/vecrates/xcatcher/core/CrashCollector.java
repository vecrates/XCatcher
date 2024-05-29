package me.vecrates.xcatcher.core;

import androidx.annotation.NonNull;

public interface CrashCollector {

    /**
     * 发生异常时触发
     *
     * @return 崩溃产生后如何处理
     */
    @CrashHandle
    int doCollect(@NonNull Throwable e, @NonNull String log);

}
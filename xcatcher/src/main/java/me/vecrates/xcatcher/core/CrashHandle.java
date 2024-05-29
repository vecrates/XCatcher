package me.vecrates.xcatcher.core;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({CrashHandle.THROW, CrashHandle.EXIT, CrashHandle.IGNORE})
public @interface CrashHandle {

    /**
     * 抛出
     */
    int THROW = 0;

    /**
     * 退出 app
     */
    int EXIT = 1;

    /**
     * 忽略
     * 特别：native 崩溃 IGNORE 无效，将当成 THROW 处理
     */
    int IGNORE = 2;

}

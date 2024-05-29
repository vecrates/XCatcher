package me.vecrates.xcatcher.core;

import androidx.annotation.NonNull;

public interface OnCrashListener {

    @CrashHandle
    int onCrash(@NonNull Throwable throwable);
}
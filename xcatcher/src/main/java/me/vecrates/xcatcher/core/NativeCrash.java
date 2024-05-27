package me.vecrates.xcatcher.core;

public class NativeCrash {

    public String threadName;
    public String message;
    public String stackTraces;

    public NativeCrash() {

    }

    public NativeCrash(String threadName, String message, String stackTraces) {
        this.threadName = threadName;
        this.message = message;
        this.stackTraces = stackTraces;
    }

}

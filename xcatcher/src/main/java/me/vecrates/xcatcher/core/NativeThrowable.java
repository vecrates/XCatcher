package me.vecrates.xcatcher.core;

public class NativeThrowable extends Throwable {

    public NativeThrowable(String nativeStack) {
        super(nativeStack);
    }

    public NativeThrowable(String nativeStack, StackTraceElement[] javaStacks) {
        super(nativeStack);
        setStackTrace(javaStacks);
    }


}

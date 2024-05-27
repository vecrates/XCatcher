package me.vecrates.xcatcher;

public interface CrashCollector {

    /**
     * 发生异常时触发
     *
     * @return 是否退出进程（目前对native崩溃无效）
     */
    boolean doCollect(Throwable e);

}
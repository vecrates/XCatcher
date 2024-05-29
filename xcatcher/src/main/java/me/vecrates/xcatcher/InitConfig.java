package me.vecrates.xcatcher;

import me.vecrates.xcatcher.core.CrashCollector;

public class InitConfig {

    /**
     * 是否捕获 native 崩溃
     */
    public final boolean catchNative;
    /**
     * 崩溃时读取的 logcat 的行数
     */
    public final int readLogcatLines;
    /**
     * 崩溃监听
     */
    public final CrashCollector collector;

    public InitConfig(boolean catchNative, int readLogcatLines, CrashCollector collector) {
        this.catchNative = catchNative;
        this.readLogcatLines = readLogcatLines;
        this.collector = collector;
    }

    public static class Builder {
        private boolean catchNative = true;
        private int readLogcatLines = 0;
        private CrashCollector collector = null;

        public Builder setCatchNative(boolean catchNative) {
            this.catchNative = catchNative;
            return this;
        }

        public Builder setReadLogcatLines(int readLogcatLines) {
            this.readLogcatLines = readLogcatLines;
            return this;
        }

        public Builder setCollector(CrashCollector collector) {
            this.collector = collector;
            return this;
        }

        public InitConfig build() {
            return new InitConfig(catchNative, readLogcatLines, collector);
        }

    }


}

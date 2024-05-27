-keep class me.vecrates.xcatcher.core.NativeCrash {*;}
-keep class me.vecrates.xcatcher.core.NativeCatcher {
public void init();
public void crash();
}
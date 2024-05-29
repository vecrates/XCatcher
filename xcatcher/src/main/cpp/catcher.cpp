/* DO NOT EDIT THIS FILE - it is machine generated */

/* Header for class me_vecrates_watcher_core_NativeCatcher */

#ifndef _Included_me_vecrates_xcatcher_core_NativeCatcher
#define _Included_me_vecrates_xcatcher_core_NativeCatcher

#include <jni.h>
#include <signal.h>
#include <locale>
#include <ios>
#include <android/log.h>
#include <cstdlib>
#include <pthread.h>
#include <unistd.h>
#include "CrashHandler.h"
#include <fstream>
#include <xunwind.h>
#include<map>

#define LOGI(...) if(!NDEBUG) __android_log_print(ANDROID_LOG_ERROR,"native_catcher",__VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

JavaVM *jvm;
CrashHandler *crashHandler;
std::map<int, struct sigaction> sigactionMap;
struct sigaction action{};

//在加载动态库时回去调用 JNI_OnLoad 方法，在这里可以得到 JavaVM 实例对象
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    return JNI_VERSION_1_6;
}

void InstallAlternateStack() {
    stack_t stack;
    memset(&stack, 0, sizeof(stack));
    /* Reserver the system default stack size. We don't need that much by the way. */
    stack.ss_size = SIGSTKSZ;
    stack.ss_sp = malloc(stack.ss_size);
    stack.ss_flags = 0;
    /* Install alternate stack size. Be sure the memory region is valid until you revert it. */
    if (stack.ss_sp != NULL && sigaltstack(&stack, NULL) == 0) {
        LOGI("alternate stack installed");
    } else {
        LOGI("alternate stack install failed");
    }
}

void SignalHandle(int sig, siginfo_t *si, void *context) {
    LOGI("handle signal: signal=%d sigCode=%d errno=%d", sig, si->si_code, si->si_errno);

    std::mutex mut;
    std::condition_variable locker;

    //todo 在不退出的情况下，如何清理掉产生的信号，否则不退出进程会一直收到sig
    crashHandler->NotifyCrash(getpid(), gettid(), si, context, &locker);

    std::unique_lock<std::mutex> lk(mut);
    locker.wait(lk, [] {
        return crashHandler == nullptr || crashHandler->isCrashHandled();
    });

    lk.unlock();

    //交给旧处理函数（如果handler退出进程，将不会执行）
    std::map<int, struct sigaction>::iterator it = sigactionMap.find(sig);
    if (it != sigactionMap.end()) {
        it->second.sa_sigaction(sig, si, context);
    }
}

void RegisterSignal() {
    memset(&action, 0, sizeof(action));
    sigemptyset(&action.sa_mask);

    /*//忽略信号量，进程不会崩溃，同时也将无法排查错误
    action.sa_handler = SIG_IGN;
    action.sa_flags = SA_RESTART;*/

    //自定义函数处理信号
    action.sa_sigaction = SignalHandle; //SA_SIGINFO 表示使用此函数
    action.sa_flags = SA_ONSTACK | SA_SIGINFO;

    //SIGKILL、SIGSTOP 不能安装
    int registerSignals[] = {SIGTRAP, SIGABRT, SIGILL, SIGBUS,
                             SIGFPE, SIGSEGV, SIGSTKFLT, SIGSYS};

    int lenSignals = sizeof(registerSignals) / sizeof(registerSignals[0]);
    for (int i = 0; i < lenSignals; ++i) {
        struct sigaction old;
        sigaction(registerSignals[i], &action, &old);
        sigactionMap[registerSignals[i]] = old;
    }
}

JNIEXPORT void JNICALL Java_me_vecrates_xcatcher_core_NativeCatcher_init
        (JNIEnv *env, jobject instance) {
    InstallAlternateStack();
    RegisterSignal();
    crashHandler = new CrashHandler(jvm, env, env->NewGlobalRef(instance));
    crashHandler->Init();
}

JNIEXPORT void JNICALL Java_me_vecrates_xcatcher_core_NativeCatcher_crash
        (JNIEnv *env, jobject instance) {
    int *p = 0; //从0开始的第一个页面的权限被设置为不可读也不可写
    *p = 1; //写空指针指向的内存，产生SIGSEGV信号
}

#ifdef __cplusplus
}
#endif
#endif
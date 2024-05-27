#include <jni.h>
#include <pthread.h>
#include <mutex>
#include "CrashHandler.h"
#include "android/log.h"
#include "xunwind.h"
#include <unistd.h>

#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"CrashHandler",FORMAT,##__VA_ARGS__);

#define THREAD_NAME_LENGTH 120


CrashHandler::CrashHandler(JavaVM *vm, JNIEnv *env, jobject jobj) {
    this->vm = vm;
    this->env = env;
    this->jobj = jobj;

    jclass jclaz = env->GetObjectClass(jobj);
    this->jmethod = env->GetMethodID(jclaz, "onCrash",
                                     "(Lme/vecrates/xcatcher/core/NativeCrash;)V");

    this->crash_cls = static_cast<jclass>(env->NewGlobalRef(
            env->FindClass("me/vecrates/xcatcher/core/NativeCrash")));
    this->crash_cons = env->GetMethodID(crash_cls, "<init>", "()V");
    this->native_crash_obj = env->NewGlobalRef(env->NewObject(crash_cls, crash_cons));

}

char *getThreadName(pid_t tid) {
    if (tid <= 1) {
        return NULL;
    }
    char *path = (char *) calloc(1, 100);
    char *line = (char *) calloc(1, THREAD_NAME_LENGTH);

    snprintf(path, PATH_MAX, "proc/%d/comm", tid);
    FILE *commFile = fopen(path, "r");
    if (commFile) {
        fgets(line, THREAD_NAME_LENGTH, commFile);
        fclose(commFile);
    }
    free(path);
    if (line) {
        int length = strlen(line);
        if (line[length - 1] == '\n') {
            line[length - 1] = '\0';
        }
    }
    return line;
}

char *getMessage(int signal, int code) {
    switch (signal) {
        case SIGABRT:
            return "abort";
        case SIGBUS:
            switch (code) {
                case BUS_ADRALN:
                    return "Invalid address alignment";
                case BUS_ADRERR:
                    return "Nonexistent physical address";
                case BUS_OBJERR:
                    return "Object-specific hardware error";
                default:
                    return "SIGBUS(unknown)";
            }
        case SIGFPE:
            switch (code) {
                case FPE_INTDIV:
                    return "Integer divide by zero";
                case FPE_INTOVF:
                    return "Integer overflow";
                case FPE_FLTDIV:
                    return "Floating-point divide by zero";
                case FPE_FLTOVF:
                    return "Floating-point overflow";
                case FPE_FLTUND:
                    return "Floating-point underflow";
                case FPE_FLTRES:
                    return "Floating-point inexact result";
                case FPE_FLTINV:
                    return "Invalid floating-point operation";
                case FPE_FLTSUB:
                    return "Subscript out of range";
                default:
                    return "Floating-point";
            }
        case SIGILL:
            switch (code) {
                case ILL_ILLOPC:
                    return "Illegal opcode";
                case ILL_ILLOPN:
                    return "Illegal operand";
                case ILL_ILLADR:
                    return "Illegal addressing mode";
                case ILL_ILLTRP:
                    return "Illegal trap";
                case ILL_PRVOPC:
                    return "Privileged opcode";
                case ILL_PRVREG:
                    return "Privileged register";
                case ILL_COPROC:
                    return "Coprocessor error";
                case ILL_BADSTK:
                    return "Internal stack error";
                default:
                    return "SIGILL(unknown)";
            }
        case SIGSEGV:
            switch (code) {
                case SEGV_MAPERR:
                    return "Address not mapped to object";
                case SEGV_ACCERR:
                    return "Invalid permissions for mapped object";
                default:
                    return "Segmentation violation";
            }
        default:
            return "unknown";
    }
}

void CrashHandler::OnCrash() {

    JNIEnv *env;
    vm->AttachCurrentThread(&env, NULL);

    char *thread_name = getThreadName(this->tid);
    jstring thread_name_str = env->NewStringUTF(thread_name);
    free(thread_name);

    char *message = getMessage(this->si->si_signo, this->si->si_code);
    jstring message_str = env->NewStringUTF(message);

    char *traces = xunwind_cfi_get(getpid(), gettid(), this->context, NULL);
    jstring traces_str = env->NewStringUTF(traces);

    jfieldID jfi = env->GetFieldID(crash_cls, "threadName", "Ljava/lang/String;");
    env->SetObjectField(native_crash_obj, jfi, thread_name_str);

    jfi = env->GetFieldID(crash_cls, "message", "Ljava/lang/String;");
    env->SetObjectField(native_crash_obj, jfi, message_str);

    jfi = env->GetFieldID(crash_cls, "stackTraces", "Ljava/lang/String;");
    env->SetObjectField(native_crash_obj, jfi, traces_str);

    env->CallVoidMethod(this->jobj, this->jmethod, native_crash_obj); //

//    env->DeleteGlobalRef(this->crash_cls);
//    env->DeleteGlobalRef(this->native_crash_obj);
//
//    //部分手机产生 signal 11
//    env->ReleaseStringUTFChars(thread_name_str, thread_name);
//    env->ReleaseStringUTFChars(message_str, message);
//    env->ReleaseStringUTFChars(traces_str, traces);

    vm->DetachCurrentThread();

    quick_exit(0);
}

void CrashHandler::NotifyCrash(pid_t pid, pid_t tid, siginfo_t *si, void *context) {
    this->crashed = true;
    this->pid = pid;
    this->tid = tid;
    this->si = si;
    this->context = context;
    this->crash_cond.notify_one();
}

CrashHandler *threadCrashHandler;

void *CrashHandler::ThreadRunnable(void *data) {
    threadCrashHandler = (CrashHandler *) data;

    //等待 crash 发生
    LOGI("crash handler >> ready");
    std::unique_lock<std::mutex> lk(threadCrashHandler->mut);
    threadCrashHandler->crash_cond.wait(lk, [] {
        return threadCrashHandler->crashed;
    });

    threadCrashHandler->OnCrash();

    lk.unlock();

    LOGI("crash handler >> finish")

    return 0;
}

void CrashHandler::Init() {
    pthread_t thread;
    int ret = pthread_create(&thread, NULL, ThreadRunnable, this);
    if (ret != 0) {
        LOGI("crash thread create failed, return %d", ret);
    }
}
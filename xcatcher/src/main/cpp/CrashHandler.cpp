#include <jni.h>
#include <pthread.h>
#include <mutex>
#include "CrashHandler.h"
#include "android/log.h"
#include "xunwind.h"
#include <unistd.h>
#include <fstream>
#include <string>

#define LOGI(FORMAT, ...) if(!NDEBUG) __android_log_print(ANDROID_LOG_ERROR,"CrashHandler",FORMAT,##__VA_ARGS__);

template<typename ... Args>
std::string stringFormat(const std::string &format, Args ... args) {
    size_t size = 1 + snprintf(nullptr, 0, format.c_str(), args ...);  // Extra space for \0
    char bytes[size];
    snprintf(bytes, size, format.c_str(), args ...);
    return {bytes};
}

CrashHandler::CrashHandler(JavaVM *vm, JNIEnv *env, jobject jobj) {
    this->vm = vm;
    this->env = env;
    this->jobj = jobj;

    jclass jclaz = env->GetObjectClass(jobj);
    this->jmethod = env->GetMethodID(jclaz, "onCrash",
                                     "(Lme/vecrates/xcatcher/core/NativeCrash;)Z");

    this->crash_cls = static_cast<jclass>(env->NewGlobalRef(
            env->FindClass("me/vecrates/xcatcher/core/NativeCrash")));
    this->crash_cons = env->GetMethodID(crash_cls, "<init>", "()V");
    this->native_crash_obj = env->NewGlobalRef(env->NewObject(crash_cls, crash_cons));

}

std::string getThreadName(pid_t tid) {
    if (tid <= 1) {
        return "unknown";
    }
    std::string path = stringFormat("proc/%d/comm", tid);
    std::ifstream inFile(path, std::ios::in);
    if (!inFile) {
        return "unknown";
    }
    std::string line;
    std::getline(inFile, line);
    inFile.close();
    return line;
}

std::string getMessage(int signal, int code, int errorNo) {
    std::string hint;

    switch (signal) {
        case SIGABRT:
            hint = "abort";
            break;
        case SIGBUS:
            switch (code) {
                case BUS_ADRALN:
                    hint = "Invalid address alignment";
                    break;
                case BUS_ADRERR:
                    hint = "Nonexistent physical address";
                    break;
                case BUS_OBJERR:
                    hint = "Object-specific hardware error";
                    break;
                default:
                    hint = "SIGBUS(unknown)";
                    break;
            }
            break;
        case SIGFPE:
            switch (code) {
                case FPE_INTDIV:
                    hint = "Integer divide by zero";
                    break;
                case FPE_INTOVF:
                    hint = "Integer overflow";
                    break;
                case FPE_FLTDIV:
                    hint = "Floating-point divide by zero";
                    break;
                case FPE_FLTOVF:
                    hint = "Floating-point overflow";
                    break;
                case FPE_FLTUND:
                    hint = "Floating-point underflow";
                    break;
                case FPE_FLTRES:
                    hint = "Floating-point inexact result";
                    break;
                case FPE_FLTINV:
                    hint = "Invalid floating-point operation";
                    break;
                case FPE_FLTSUB:
                    hint = "Subscript out of range";
                    break;
                default:
                    hint = "Floating-point";
                    break;
            }
            break;
        case SIGILL:
            switch (code) {
                case ILL_ILLOPC:
                    hint = "Illegal opcode";
                    break;
                case ILL_ILLOPN:
                    hint = "Illegal operand";
                    break;
                case ILL_ILLADR:
                    hint = "Illegal addressing mode";
                    break;
                case ILL_ILLTRP:
                    hint = "Illegal trap";
                    break;
                case ILL_PRVOPC:
                    hint = "Privileged opcode";
                    break;
                case ILL_PRVREG:
                    hint = "Privileged register";
                    break;
                case ILL_COPROC:
                    hint = "Coprocessor error";
                    break;
                case ILL_BADSTK:
                    hint = "Internal stack error";
                    break;
                default:
                    hint = "SIGILL(unknown)";
                    break;
            }
            break;
        case SIGSEGV:
            switch (code) {
                case SEGV_MAPERR:
                    hint = "Address not mapped to object";
                    break;
                case SEGV_ACCERR:
                    hint = "Invalid permissions for mapped object";
                    break;
                default:
                    hint = "Segmentation violation";
                    break;
            }
            break;
        default:
            hint = "<unknown>";
    }

    std::string message = stringFormat("signal=%d,code=%d,errno=%d (%s)",
                                       signal, code, errorNo, hint.c_str());
    return message;
}

void CrashHandler::OnCrash() {
    JNIEnv *env;
    vm->AttachCurrentThread(&env, NULL);

    std::string threadName = getThreadName(this->tid);
    jstring thread_name_str = env->NewStringUTF(threadName.c_str());

    std::string message = getMessage(this->si->si_signo, this->si->si_code, this->si->si_errno);
    jstring message_str = env->NewStringUTF(message.c_str());

    char *traces = xunwind_cfi_get(this->pid, this->tid, this->context, NULL);
    jstring traces_str = env->NewStringUTF(traces);

    jfieldID jfi = env->GetFieldID(crash_cls, "threadName", "Ljava/lang/String;");
    env->SetObjectField(native_crash_obj, jfi, thread_name_str);

    jfi = env->GetFieldID(crash_cls, "message", "Ljava/lang/String;");
    env->SetObjectField(native_crash_obj, jfi, message_str);

    jfi = env->GetFieldID(crash_cls, "stackTraces", "Ljava/lang/String;");
    env->SetObjectField(native_crash_obj, jfi, traces_str);

    jboolean quitProcess = env->CallBooleanMethod(this->jobj, this->jmethod, native_crash_obj); //

    //free(traces);

//    env->DeleteGlobalRef(this->crash_cls);
//    env->DeleteGlobalRef(this->native_crash_obj);
//
//    //部分手机产生 signal 11，后续处理都是结束进程，故不释放
//    env->ReleaseStringUTFChars(thread_name_str, thread_name);
//    env->ReleaseStringUTFChars(message_str, message);
//    env->ReleaseStringUTFChars(traces_str, traces);

    vm->DetachCurrentThread();

    if (quitProcess) {
        quick_exit(0);
    }

}

bool CrashHandler::isCrashHandled() {
    return crashHandled;
}

void CrashHandler::NotifyCrash(pid_t pid,
                               pid_t tid,
                               siginfo_t *si,
                               void *context,
                               std::condition_variable *locker) {
    this->crashed = true;
    this->pid = pid;
    this->tid = tid;
    this->si = si;
    this->context = context;
    this->waitCrashHandledLocker = locker;
    this->crashHandled = false;
    this->crash_cond.notify_one();
}

CrashHandler *threadCrashHandler;

void *CrashHandler::ThreadRunnable(void *data) {
    threadCrashHandler = (CrashHandler *) data;

    //等待 crash 发生
    LOGI("crash handler >>> ready");

    std::unique_lock<std::mutex> lk(threadCrashHandler->mut);
    threadCrashHandler->crash_cond.wait(lk, [] {
        return threadCrashHandler->crashed;
    });

    threadCrashHandler->OnCrash();

    lk.unlock();

    threadCrashHandler->crashHandled = true;
    if (threadCrashHandler->waitCrashHandledLocker != nullptr) {
        threadCrashHandler->waitCrashHandledLocker->notify_one();
    }

    LOGI("crash handler >>> finish");

    return 0;
}

void CrashHandler::Init() {
    pthread_t thread;
    int ret = pthread_create(&thread, NULL, ThreadRunnable, this);
    if (ret != 0) {
        LOGI("crash thread create failed, return %d", ret);
    }
}

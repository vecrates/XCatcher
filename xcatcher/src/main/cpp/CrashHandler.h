#include <sys/types.h>

class CrashHandler {

public:
    JavaVM *vm;
    JNIEnv *env;
    jobject jobj;
    jmethodID jmethod;

private:
    std::mutex mut;
    std::condition_variable crash_cond;
    bool crashed;
    siginfo_t *si;
    pid_t pid;
    pid_t tid;
    void *context;
    jclass crash_cls;
    jmethodID crash_cons;
    jobject native_crash_obj;

    //等待此 handler 处理完成
    std::condition_variable *waitCrashHandledLocker = nullptr;
    bool crashHandled = false;

public:
    CrashHandler(JavaVM *vm, JNIEnv *env, jobject jobj);

    //析构函数
    ~CrashHandler();

    void NotifyCrash(pid_t pid,
                     pid_t tid,
                     siginfo_t *si,
                     void *context,
                     std::condition_variable *locker);

    void Init();

    bool isCrashHandled();

private:
    static void *ThreadRunnable(void *data);

    void OnCrash();

};


package top.dreamlike.panama.uring.helper.unsafe;


import java.lang.foreign.MemorySegment;

class GlobalRef implements AutoCloseable {

    private final MemorySegment globalRef;
    private final JNIEnv env;

    public final boolean isRef;

    public final JValue jValue;

    public GlobalRef(JNIEnv env, MemorySegment jobject) {
        this.env = env;
        globalRef = env.NewGlobalRef(jobject);
        isRef = true;
        jValue = new JValue(globalRef.address());
    }

    public GlobalRef(JNIEnv env, JValue jobject) {
        this.env = env;
        isRef = false;
        jValue = jobject;
        globalRef = null;
    }

    public MemorySegment ref() {
        return globalRef;
    }

    public JValue jValue() {
        return jValue;
    }

    @Override
    public void close() throws Exception {
        if (isRef) {
            env.DeleteGlobalRef(globalRef);
        }
    }
}

package top.dreamlike.panama.uring.helper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

public class CloseHandle {
    Runnable closeHandle;

    public CloseHandle(Runnable closeHandle) {
        Objects.requireNonNull(closeHandle);
        this.closeHandle = closeHandle;
    }

    public void close() {
        if (closeHandle == null) {
            return;
        }

        Runnable closeHandle = this.closeHandle;
        if (CLOSE_HANDLE_VH.compareAndSet(this, closeHandle, null)) {
            closeHandle.run();
        }
    }

    private static final VarHandle CLOSE_HANDLE_VH;

    static {
        try {
            var lookup = MethodHandles.lookup();
            CLOSE_HANDLE_VH = lookup.findVarHandle(CloseHandle.class, "closeHandle", Runnable.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}

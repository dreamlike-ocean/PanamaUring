package top.dreamlike.helper;

import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.nativeLib.inet.inet_h;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

public class DebugHelper {
    private static final VarHandle IO_URING_VAR_HANDLER;
    public static int connect(int fd, MemorySegment sockaddrSegement){
        int connect_res = inet_h.connect(fd, sockaddrSegement, (int)sockaddrSegement.byteSize());
//        这个返回0
        System.out.println("connect_res :"+connect_res);
        System.out.println("Err:"+NativeHelper.getNowError());
        return connect_res;
    }

    @Unsafe("反射内部实现，测试使用")
    public IOUring getIOUring(IOUringEventLoop IOUringEventLoop){
        return (IOUring) IO_URING_VAR_HANDLER.get(IOUringEventLoop);
    }

    static {
        try {
            //不破坏封装
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Field field = IOUringEventLoop.class.getDeclaredField("ioUring");
            field.setAccessible(true);
            IO_URING_VAR_HANDLER = MethodHandles.privateLookupIn(IOUringEventLoop.class, lookup)
                    .unreflectVarHandle(field);
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }
}

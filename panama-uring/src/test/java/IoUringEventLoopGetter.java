import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.eventloop.VTIoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;

import java.util.function.Consumer;

public class IoUringEventLoopGetter {

    public static IoUringEventLoop get(boolean vt,Consumer<IoUringParams> ioUringParamsFactory) {
        return vt ? new VTIoUringEventLoop(ioUringParamsFactory) : new IoUringEventLoop(ioUringParamsFactory);
    }

}

package top.dreamlike.panama.uring.networking.eventloop;

import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.LambdaHelper;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.UnsafeHelper;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufRingSetupResult;
import top.dreamlike.panama.uring.networking.stream.IOStream;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;

public class ReaderEventLoop extends IoUringEventLoop {

    private final LongFunction<IoUringBufferRing> choose;

    public ReaderEventLoop(ReadEventLoopOption option) {
        super(option.toConfig());
        choose = option.getChooser().apply(this);
    }

    public void register(IOStream stream) {

    }

    public static class DefaultBufferRingChoose implements LongFunction<IoUringBufferRing> {
        private final static AtomicInteger count = new AtomicInteger();

        private final static long MAX_DIRECT_LIMIT;

        private final IoUringBufferRing bufferRing;

        public DefaultBufferRingChoose(long total, IoUringEventLoop eventLoop) {
            if (total % 4 * 1024 != 0) {
                throw new IllegalArgumentException("total must be multiple of 4KB");
            }
            long entries = total / (4 * 1024);
            //entries 必须是2的幂
            if ((entries & (entries - 1)) != 0) {
                throw new IllegalArgumentException("entries must be power of 2");
            }
            int bufferGroupId = count.getAndIncrement();
            IoUringBufRingSetupResult ringSetupResult = LambdaHelper.runWithThrowable(() -> eventLoop.setupBufferRing((int) entries, 4 * 1024, ((short) bufferGroupId)).get());
            int res = ringSetupResult.res();
            if (res < 0) {
                throw new SyscallException(res);
            }
            bufferRing = ringSetupResult.bufRing();
        }

        DefaultBufferRingChoose(IoUringEventLoop eventLoop) {
            this(MAX_DIRECT_LIMIT, eventLoop);
        }

        @Override
        public IoUringBufferRing apply(long value) {
            return bufferRing;
        }

        static {
            long maxDirectMemory = -1;
            String property = System.getProperty("panamauring.networking.bufferRingTotalSize", "");
            if (!property.isBlank()) {
                try {
                    maxDirectMemory = Long.parseLong(property);
                } catch (NumberFormatException _) {

                }
            }

            if (maxDirectMemory == -1) {
                maxDirectMemory = UnsafeHelper.getMaxDirectMemory();
            }
            MAX_DIRECT_LIMIT = maxDirectMemory;

        }


    }


}

package top.dreamlike.panama.generator.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import top.dreamlike.panama.generator.proxy.ErrorNo;
import top.dreamlike.panama.generator.proxy.MemoryLifetimeScope;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.generator.test.call.LibPerson;
import top.dreamlike.panama.generator.test.struct.Person;

import java.lang.foreign.*;
import java.util.ArrayList;
import java.util.List;

public class ErrorNoTest {
    private static StructProxyGenerator structProxyGenerator;

    private static NativeCallGenerator callGenerator;

    private static LibPerson libPerson;

    @BeforeClass
    public static void init() {
        structProxyGenerator = new StructProxyGenerator();
        callGenerator = new NativeCallGenerator(structProxyGenerator);
        callGenerator.indyMode();
        libPerson = callGenerator.generate(LibPerson.class);
    }

    @Test
    public void testError() throws Exception {
        try(MemoryLifetimeScope scope = MemoryLifetimeScope.local()) {
            long res = libPerson.set_error_no(999, 2);
            Assert.assertEquals(res, 2);
            Assert.assertEquals(ErrorNo.error.get().intValue(), 999);
        }

        try (Arena arena = Arena.ofConfined()) {
            MemoryLifetimeScope.of(arena)
                    .active(() -> {
                        long res1 = libPerson.set_error_no(888, 2);
                        Assert.assertEquals(res1, 2);
                        Assert.assertEquals(ErrorNo.error.get().intValue(), 888);
                        ErrorNo.CapturedErrorState capturedError = ErrorNo.getCapturedError();
                        Assert.assertEquals(capturedError.errno(), 888);
                    });
        }

    }

    @Test
    public void testReturnStruct() {
        MemoryLayout rawCStructLayout = structProxyGenerator.extract(Person.class);
        long size = rawCStructLayout.byteSize();
        RecordProxyAllocator recordProxyAllocator = new RecordProxyAllocator(Arena.ofConfined());
        try (MemoryLifetimeScope _ = MemoryLifetimeScope.of(recordProxyAllocator)) {
            Person person = libPerson.returnStruct(1, 2);
            Assert.assertEquals(1, person.getA());
            Assert.assertEquals(2, person.getN());
            Assert.assertEquals(1, recordProxyAllocator.allocatorInfoList.size());
            Assert.assertEquals(size, recordProxyAllocator.allocatorInfoList.get(0).byteSize());
            recordProxyAllocator.allocatorInfoList.clear();

            int errorNo = 999;
            person = libPerson.returnStructAndErrorNo(1, 2, errorNo);
            Assert.assertEquals(1, person.getA());
            Assert.assertEquals(2, person.getN());
            Assert.assertEquals(errorNo, ErrorNo.error.get().intValue());
            Assert.assertEquals(2, recordProxyAllocator.allocatorInfoList.size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class RecordProxyAllocator implements SegmentAllocator {
        private final Arena arena;
        public record AllocatorInfo(long byteSize, long byteAlignment) {
        }
        private final List<AllocatorInfo> allocatorInfoList = new ArrayList<>();
        private RecordProxyAllocator(Arena arena) {
            this.arena = arena;
        }

        @Override
        public MemorySegment allocate(long byteSize, long byteAlignment) {
            allocatorInfoList.add(new AllocatorInfo(byteSize, byteAlignment));
            return arena.allocate(byteSize, byteAlignment);
        }
    }
}

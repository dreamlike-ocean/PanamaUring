package top.dreamlike.panama.generator.proxy;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;

public class NativeImageTestMain {
    static void main() {
        StructProxyGenerator structProxyGenerator = new StructProxyGenerator();
        NativeCallGenerator nativeCallGenerator = new NativeCallGenerator(structProxyGenerator);
        NativeLib ffi = nativeCallGenerator.generate(NativeLib.class);
        int pageSize = ffi.getPageSize();
        int fatPage = ffi.fastPage();

        if (pageSize != fatPage) {
            throw new RuntimeException("page size not equal fat page size");
        }

        MemorySegment rawAddFp = SymbolLookup.loaderLookup()
                .find("raw_add").get();
        int result = ffi.rawAdd(rawAddFp, 1, 2);
        if (result != 3) {
            throw new RuntimeException("raw add not equal 3");
        }
        Person person = ffi.fillPerson(2001, 2000);
        if (person.getA() != 2001 || person.getN() != 2000) {
            throw new RuntimeException("fill person not equal 2001,2000");
        }

        EpollEvent epollEvent = structProxyGenerator.allocate(Arena.global(), EpollEvent.class);
        EpollEventData epollEventData = structProxyGenerator.allocate(Arena.global(), EpollEventData.class);
        epollEventData.setU64(123);
        epollEvent.setEventData(epollEventData);
        Shortcut shortcut = structProxyGenerator.generateShortcut(Shortcut.class).get();
        if (shortcut.getU64(epollEvent) != 123) {
            throw new RuntimeException("get u64 not equal 123");
        }

        shortcut.setU64(epollEvent, 456);
        if (shortcut.getU64(epollEvent) != 456) {
            throw new RuntimeException("set u64 not equal 456");
        }
    }
}

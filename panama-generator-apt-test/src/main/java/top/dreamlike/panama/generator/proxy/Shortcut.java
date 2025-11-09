package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.annotation.CompileTimeGenerate;
import top.dreamlike.panama.generator.annotation.ShortcutOption;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

@CompileTimeGenerate(CompileTimeGenerate.GenerateType.SHORTCUT)
public interface Shortcut {

    @ShortcutOption(value = {"eventData", "u64"}, owner = EpollEvent.class, mode = VarHandle.AccessMode.GET)
    long getU64(EpollEvent epollEvent);

    @ShortcutOption(value = {"eventData", "u64"}, owner = EpollEvent.class, mode = VarHandle.AccessMode.GET)
    long getU64(MemorySegment eventGenerated);

    @ShortcutOption(value = {"eventData", "ptr"}, owner = EpollEvent.class, mode = VarHandle.AccessMode.GET)
    long getPtr(EpollEvent eventGenerated);

    @ShortcutOption(value = {"eventData", "u64"}, owner = EpollEvent.class, mode = VarHandle.AccessMode.SET)
    void setU64(EpollEvent epollEvent, long u64);

}

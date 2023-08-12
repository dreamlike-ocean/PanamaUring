module top.dremalike.panama.uring.linux.x86_64 {
    exports top.dreamlike.async.uring;
    exports top.dreamlike.async.file;
    exports top.dreamlike.async.socket;
    exports top.dreamlike.async;
    exports top.dreamlike.eventloop;
    exports top.dreamlike.extension.memory;
    requires jctools.core;
    requires jdk.unsupported;
    requires top.dreamlike.nativeLib;
    requires org.slf4j;
    requires io.smallrye.mutiny;
}
module panama.uring.linux.x86_64 {
    exports top.dreamlike.async.uring;
    exports top.dreamlike.async.file;
    exports top.dreamlike.async.socket;
    exports top.dreamlike.async;
    requires jctools.core;
    requires jdk.unsupported;
    requires org.slf4j;
    requires nativeLib;
}
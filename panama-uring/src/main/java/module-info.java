module panama.uring.linux.x86_64 {
    exports top.dreamlike.async.uring;
    exports top.dreamlike.async.file;
    exports top.dreamlike.async.socket;
    exports top.dreamlike.helper;
    exports top.dreamlike.nativeLib.fcntl;
    requires jctools.core;
    requires jdk.unsupported;
}
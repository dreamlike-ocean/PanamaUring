package top.dreamlike.async.uring;

public enum Op {
    CONNECT,
    ACCEPT,
    MULTI_SHOT,
    FILE_READ,
    FILE_SELECTED_READ,
    FILE_WRITE,
    FILE_SYNC,
    SOCKET_READ,
    SOCKET_SELECTED_READ,
    SOCKET_WRITE,
    NO,
    TIMEOUT
}

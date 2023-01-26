package top.dreamlike.helper;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;

public enum FileOp {
    CREATE(O_CREAT()),
    CREATE_NEW(O_CREAT() | O_TRUNC()),
    READ_ONLY(O_RDONLY()),
    RW(O_RDWR()),
    APPEDN(O_APPEND()),
    DIRECT(__O_DIRECT()),
    O_DSYNC(O_DSYNC()),
    WRITE_ONLY(O_WRONLY());

    public int op;

    FileOp(int op) {
        this.op = op;
    }
}

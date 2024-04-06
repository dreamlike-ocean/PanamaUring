package top.dreamlike.panama.uring.sync.fd;

import top.dreamlike.panama.uring.sync.trait.NativeFd;
import top.dreamlike.panama.uring.sync.trait.PollableFd;

public class StdInputFd implements NativeFd, PollableFd {
    @Override
    public int fd() {
        return 0;
    }

    @Override
    public int writeFd() {
        throw new UnsupportedOperationException("StdInputFd is not writable");
    }
}

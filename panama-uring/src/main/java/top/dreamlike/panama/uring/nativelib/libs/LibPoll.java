package top.dreamlike.panama.uring.nativelib.libs;

public interface LibPoll {
    int POLLIN = 0x001;        /* There is data to read.  */
    int POLLPRI = 0x002;        /* There is urgent data to read.  */
    int POLLOUT = 0x004;        /* Writing now will not block.  */

    int POLLRDNORM = 0x040;    /* Normal data may be read.  */
    int POLLRDBAND = 0x080;    /* Priority data may be read.  */
    int POLLWRNORM = 0x100;    /* Writing now will not block.  */
    int POLLWRBAND = 0x200;    /* Priority data may be written.  */
    int POLLMSG = 0x400;
    int POLLREMOVE = 0x1000;
    int POLLRDHUP = 0x2000;
    int POLLERR = 0x008;    /* Error condition.  */
    int POLLHUP = 0x010;    /* Hung up.  */
    int POLLNVAL = 0x020;  /* Invalid polling request.  */
}

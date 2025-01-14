package top.dreamlike.panama.uring.nativelib.libs;

import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;

import java.lang.foreign.MemorySegment;

public interface Libc {
    int open(MemorySegment pathname, int flags);

    int read(int fd, MemorySegment buf, int count);

    int write(int fd, MemorySegment buf, int count);

    int close(int fd);

    int socket(int domain, int type, int protocol);

    int eventfd(int initVal, int flags);

    int eventfd_read(int fd,/* uint64_t* value  */@Pointer MemorySegment value);

    int eventfd_write(int fd, long value);

    @NativeFunction(value = "__errno_location")
    MemorySegment errorNo();

    int dup(int fd);

    MemorySegment strerror(int errno);

    int fcntl(int fd, int cmd, int arg);

    int bind(int fd, @Pointer MemorySegment addr, int addrlen);

    int listen(int socketFd, int backlog);

    int connect(int socketFd,@Pointer MemorySegment addr, int addrLen);

    int getsockname(int socketFd, @Pointer MemorySegment addr, @Pointer MemorySegment addrlen);

    int getpeername(int socketFd, @Pointer MemorySegment addr, @Pointer MemorySegment addrlen);

    int pipe(@Pointer MemorySegment pipeFd);

    int setsockopt(int sockfd, int level, int optname, @Pointer MemorySegment optval, int optlen);

    int getpagesize();

    int inotify_init();

    int inotify_init1(int flags);

    int inotify_add_watch(int fd, @Pointer MemorySegment name, int mask);

    int inotify_rm_watch(int fd, int wd);

    interface Inotify_H {
        int IN_NONBLOCK = 2048;
        int IN_CLOSEXEC = 524288;

        /* Supported events suitable for MASK parameter of INOTIFY_ADD_WATCH.  */
        int IN_ACCESS = 0x00000001;/* File was accessed.  */
        int IN_MODIFY = 0x00000002;/* File was modified.  */
        int IN_ATTRIB = 0x00000004;/* Metadata changed.  */
        int IN_CLOSE_WRITE = 0x00000008;/* Writtable file was closed.  */
        int IN_CLOSE_NOWRITE = 0x00000010;/* Unwrittable file closed.  */
        int IN_CLOSE = (IN_CLOSE_WRITE | IN_CLOSE_NOWRITE);/* Close.  */
        int IN_OPEN = 0x00000020;/* File was opened.  */
        int IN_MOVED_FROM = 0x00000040;/* File was moved from X.  */
        int IN_MOVED_TO = 0x00000080;/* File was moved to Y.  */
        int IN_MOVE = (IN_MOVED_FROM | IN_MOVED_TO);/* Moves.  */
        int IN_CREATE = 0x00000100;/* Subfile was created.  */
        int IN_DELETE = 0x00000200;/* Subfile was deleted.  */
        int IN_DELETE_SELF = 0x00000400;/* Self was deleted.  */
        int IN_MOVE_SELF = 0x00000800;/* Self was moved.  */

        /* Events sent by the kernel.  */
        int IN_UNMOUNT = 0x00002000;/* Backing fs was unmounted.  */
        int IN_Q_OVERFLOW = 0x00004000;/* Event queued overflowed.  */
        int IN_IGNORED = 0x00008000;/* File was ignored.  */


        /* Special flags.  */
        int IN_ONLYDIR = 0x01000000;/* Only watch the path if it is a
					   directory.  */
        int IN_DONT_FOLLOW = 0x02000000;/* Do not follow a sym link.  */
        int IN_EXCL_UNLINK = 0x04000000;/* Exclude events on unlinked
					   objects.  */
        int IN_MASK_CREATE = 0x10000000;/* Only create watches.  */
        int IN_MASK_ADD = 0x20000000;/* Add to the mask of an already
					   existing watch.  */
        int IN_ISDIR = 0x40000000;/* Event occurred against dir.  */
        int IN_ONESHOT = 0x80000000;/* Only send event once.  */
    }


    interface Fcntl_H {
        int O_RDONLY = 0;
        int O_WRONLY = 1;
        int O_RDWR = 2;
        int O_CREAT = 64;
        int O_EXCL = 128;
        int O_TURNC = 512;
        int O_APPEND = 1024;
        int O_NONBLOCK = 2048;
        int O_SYNC = 1052672;
        int O_ASYNC = 8192;
        int O_DIRECT = 16384;
        int O_DIRECTORY = 65536;
        int O_NOFOLLOW = 131072;
        int O_CLOEXEC = 524288;
        int O_PATH = 2097152;

        int SPLICE_F_MOVE = 1;
        int SPLICE_F_NONBLOCK = 2;
        int SPLICE_F_MORE = 4;
        int SPLICE_F_GIFT = 8;

        int F_DUPFD = 0;
        /* Duplicate file descriptor.  */
        int F_GETFD = 1;
        /* Get file descriptor flags.  */
        int F_SETFD = 2;
        /* Set file descriptor flags.  */
        int F_GETFL = 3;
        /* Get file status flags.  */
        int F_SETFL = 4;
        /* Set file status flags.  */
    }

    interface Socket_H {
        interface Domain {
            int AF_UNIX = 1;
            int AF_LOCAL = 1;
            int AF_INET = 2;
            int AF_AX25 = 3;
            int AF_IPX = 4;
            int AF_APPLETALK = 5;
            int AF_X25 = 9;
            int AF_INET6 = 10;
            int AF_DECnet = 12;
            int AF_KEY = 15;
            int AF_NETLINK = 16;
            int AF_PACKET = 17;
            int AF_RDS = 21;
            int AF_PPPOX = 24;
            int AF_LLC = 26;
            int AF_IB = 27;
            int AF_MPLS = 28;
            int AF_CAN = 29;
            int AF_TIPC = 30;
            int AF_BLUETOOTH = 31;
            int AF_IUCV = 32;
            int AF_RXRPC = 33;
            int AF_ISDN = 34;
            int AF_PHONET = 35;
            int AF_IEEE802154 = 36;
            int AF_CAIF = 37;
            int AF_ALG = 38;
            int AF_NFC = 39;
            int AF_VSOCK = 40;
            int AF_KCM = 41;
            int PF_QIPCRTR = 42;
            int AF_SMC = 43;
            int AF_XDP = 44;
            int AF_MCTP = 45;
            int AF_MAX = 46;

        }

        interface Type {
            int SOCK_STREAM = 1;
            int SOCK_DGRAM = 2;
            int SOCK_RAW = 3;
            int SOCK_RDM = 4;
            int SOCK_SEQPACKET = 5;
            int SOCK_DCCP = 6;
            int SOCK_PACKET = 10;
            int SOCK_CLOEXEC = 02000000;
            int SOCK_NONBLOCK = 04000;
        }

        interface SetSockOpt {
            int SOL_SOCKET = 1;
        }

        interface OptName {
            //       ;/* For setsockopt(2) */
            int SOL_SOCKET = 1;
            int SO_DEBUG = 1;
            int SO_REUSEADDR = 2;
            int SO_TYPE = 3;
            int SO_ERROR = 4;
            int SO_DONTROUTE = 5;
            int SO_BROADCAST = 6;
            int SO_SNDBUF = 7;
            int SO_RCVBUF = 8;
            int SO_SNDBUFFORCE = 32;
            int SO_RCVBUFFORCE = 33;
            int SO_KEEPALIVE = 9;
            int SO_OOBINLINE = 10;
            int SO_NO_CHECK = 11;
            int SO_PRIORITY = 12;
            int SO_LINGER = 13;
            int SO_BSDCOMPAT = 14;
            int SO_REUSEPORT = 15;
            int SO_PASSCRED = 16;
            int SO_PEERCRED = 17;
            int SO_RCVLOWAT = 18;
            int SO_SNDLOWAT = 19;
            int SO_RCVTIMEO_OLD = 20;
            int SO_SNDTIMEO_OLD = 21;
        }

        interface Flag {
            int MSG_DONTWAIT = 0x40;
        }
    }

    interface EventFd_H {
        int EFD_SEMAPHORE = 1;
        int EFD_CLOEXEC = 524288;
        int EFD_NONBLOCK = 2048;
    }

    interface Error_H {
        int EPERM = 1;
        /* Operation not permitted */
        int ENOENT = 2;
        /* No such file or directory */
        int ESRCH = 3;
        /* No such process */
        int EINTR = 4;
        /* Interrupted system call */
        int EIO = 5;
        /* I/O error */
        int ENXIO = 6;
        /* No such device or address */
        int E2BIG = 7;
        /* Argument list too long */
        int ENOEXEC = 8;
        /* Exec format error */
        int EBADF = 9;
        /* Bad file number */
        int ECHILD = 10;
        /* No child processes */
        int EAGAIN = 11;
        /* Try again */
        int ENOMEM = 12;
        /* Out of memory */
        int EACCES = 13;
        /* Permission denied */
        int EFAULT = 14;
        /* Bad address */
        int ENOTBLK = 15;
        /* Block device required */
        int EBUSY = 16;
        /* Device or resource busy */
        int EEXIST = 17;
        /* File exists */
        int EXDEV = 18;
        /* Cross-device link */
        int ENODEV = 19;
        /* No such device */
        int ENOTDIR = 20;
        /* Not a directory */
        int EISDIR = 21;
        /* Is a directory */
        int EINVAL = 22;
        /* Invalid argument */
        int ENFILE = 23;
        /* File table overflow */
        int EMFILE = 24;
        /* Too many open files */
        int ENOTTY = 25;
        /* Not a typewriter */
        int ETXTBSY = 26;
        /* Text file busy */
        int EFBIG = 27;
        /* File too large */
        int ENOSPC = 28;
        /* No space left on device */
        int ESPIPE = 29;
        /* Illegal seek */
        int EROFS = 30;
        /* Read-only file system */
        int EMLINK = 31;
        /* Too many links */
        int EPIPE = 32;
        /* Broken pipe */
        int EDOM = 33;
        /* Math argument out of domain of func */
        int ERANGE = 34;
        /* Math value not representable */

        int EDEADLK = 35;
        /* Resource deadlock would occur */
        int ENAMETOOLONG = 36;
        /* File name too long */
        int ENOLCK = 37;
        /* No record locks available */

        int ENOSYS = 38;
        /* Invalid system call number */
        int ENOTEMPTY = 39;
        /* Directory not empty */
        int ELOOP = 40;
        /* Too many symbolic links encountered */
        int EWOULDBLOCK = EAGAIN;
        /* Operation would block */
        int ENOMSG = 42;
        /* No message of desired type */
        int EIDRM = 43;
        /* Identifier removed */
        int ECHRNG = 44;
        /* Channel number out of range */
        int EL2NSYNC = 45;
        /* Level 2 not synchronized */
        int EL3HLT = 46;
        /* Level 3 halted */
        int EL3RST = 47;
        /* Level 3 reset */
        int ELNRNG = 48;
        /* Link number out of range */
        int EUNATCH = 49;
        /* Protocol driver not attached */
        int ENOCSI = 50;
        /* No CSI structure available */
        int EL2HLT = 51;
        /* Level 2 halted */
        int EBADE = 52;
        /* Invalid exchange */
        int EBADR = 53;
        /* Invalid request descriptor */
        int EXFULL = 54;
        /* Exchange full */
        int ENOANO = 55;
        /* No anode */
        int EBADRQC = 56;
        /* Invalid request code */
        int EBADSLT = 57;
        /* Invalid slot */
        int EDEADLOCK = EDEADLK;
        int EBFONT = 59;
        /* Bad font file format */
        int ENOSTR = 60;
        /* Device not a stream */
        int ENODATA = 61;
        /* No data available */
        int ETIME = 62;
        /* Timer expired */
        int ENOSR = 63;
        /* Out of streams resources */
        int ENONET = 64;
        /* Machine is not on the network */
        int ENOPKG = 65;
        /* Package not installed */
        int EREMOTE = 66;
        /* Object is remote */
        int ENOLINK = 67;
        /* Link has been severed */
        int EADV = 68;
        /* Advertise error */
        int ESRMNT = 69;
        /* Srmount error */
        int ECOMM = 70;
        /* Communication error on send */
        int EPROTO = 71;
        /* Protocol error */
        int EMULTIHOP = 72;
        /* Multihop attempted */
        int EDOTDOT = 73;
        /* RFS specific error */
        int EBADMSG = 74;
        /* Not a data message */
        int EOVERFLOW = 75;
        /* Value too large for defined data type */
        int ENOTUNIQ = 76;
        /* Name not unique on network */
        int EBADFD = 77;
        /* File descriptor in bad state */
        int EREMCHG = 78;
        /* Remote address changed */
        int ELIBACC = 79;
        /* Can not access a needed shared library */
        int ELIBBAD = 80;
        /* Accessing a corrupted shared library */
        int ELIBSCN = 81;
        /* .lib section in a.out corrupted */
        int ELIBMAX = 82;
        /* Attempting to link in too many shared libraries */
        int ELIBEXEC = 83;
        /* Cannot exec a shared library directly */
        int EILSEQ = 84;
        /* Illegal byte sequence */
        int ERESTART = 85;
        /* Interrupted system call should be restarted */
        int ESTRPIPE = 86;
        /* Streams pipe error */
        int EUSERS = 87;
        /* Too many users */
        int ENOTSOCK = 88;
        /* Socket operation on non-socket */
        int EDESTADDRREQ = 89;
        /* Destination address required */
        int EMSGSIZE = 90;
        /* Message too long */
        int EPROTOTYPE = 91;
        /* Protocol wrong type for socket */
        int ENOPROTOOPT = 92;
        /* Protocol not available */
        int EPROTONOSUPPORT = 93;
        /* Protocol not supported */
        int ESOCKTNOSUPPORT = 94;
        /* Socket type not supported */
        int EOPNOTSUPP = 95;
        /* Operation not supported on transport endpoint */
        int EPFNOSUPPORT = 96;
        /* Protocol family not supported */
        int EAFNOSUPPORT = 97;
        /* Address family not supported by protocol */
        int EADDRINUSE = 98;
        /* Address already in use */
        int EADDRNOTAVAIL = 99;
        /* Cannot assign requested address */
        int ENETDOWN = 100;
        /* Network is down */
        int ENETUNREACH = 101;
        /* Network is unreachable */
        int ENETRESET = 102;
        /* Network dropped connection because of reset */
        int ECONNABORTED = 103;
        /* Software caused connection abort */
        int ECONNRESET = 104;
        /* Connection reset by peer */
        int ENOBUFS = 105;
        /* No buffer space available */
        int EISCONN = 106;
        /* Transport endpoint is already connected */
        int ENOTCONN = 107;
        /* Transport endpoint is not connected */
        int ESHUTDOWN = 108;
        /* Cannot send after transport endpoint shutdown */
        int ETOOMANYREFS = 109;
        /* Too many references: cannot splice */
        int ETIMEDOUT = 110;
        /* Connection timed out */
        int ECONNREFUSED = 111;
        /* Connection refused */
        int EHOSTDOWN = 112;
        /* Host is down */
        int EHOSTUNREACH = 113;
        /* No route to host */
        int EALREADY = 114;
        /* Operation already in progress */
        int EINPROGRESS = 115;
        /* Operation now in progress */
        int ESTALE = 116;
        /* Stale file handle */
        int EUCLEAN = 117;
        /* Structure needs cleaning */
        int ENOTNAM = 118;
        /* Not a XENIX named type file */
        int ENAVAIL = 119;
        /* No XENIX semaphores available */
        int EISNAM = 120;
        /* Is a named type file */
        int EREMOTEIO = 121;
        /* Remote I/O error */
        int EDQUOT = 122;
        /* Quota exceeded */
        int ENOMEDIUM = 123;
        /* No medium found */
        int EMEDIUMTYPE = 124;
        /* Wrong medium type */
        int ECANCELED = 125;
        /* Operation Canceled */
        int ENOKEY = 126;
        /* Required key not available */
        int EKEYEXPIRED = 127;
        /* Key has expired */
        int EKEYREVOKED = 128;
        /* Key has been revoked */
        int EKEYREJECTED = 129;
        /* Key was rejected by service */
        int EOWNERDEAD = 130;
        /* Owner died */
        int ERFKILL = 132;
        /* Operation not possible due to RF-kill */
        int EHWPOISON = 133;
        /* Memory page has hardware error */
        int ENOTRECOVERABLE = 131;
        /* State not recoverable */
    }
}
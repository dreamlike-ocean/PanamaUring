package top.dreamlike.panama.generator.test.struct;

import top.dreamlike.panama.generator.annotation.Alignment;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.annotation.Union;

public class EpollEventGenerated {
    private int events;

    private epoll_data_t data;

    public epoll_data_t getData() {
        return data;
    }

    public int getEvents() {
        return events;
    }

    public void setEvents(int events) {
        this.events = events;
    }


    @Union
    @Alignment(byteSize = 1)
    public static class epoll_data_t {
        @Pointer
        private long ptr;

        private int fd;

        private int u32;

        private long u64;

        public int getFd() {
            return fd;
        }

        public void setFd(int fd) {
            this.fd = fd;
        }
    }


}

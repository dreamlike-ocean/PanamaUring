package top.dreamlike.epoll.extension;

import java.util.Objects;
import java.util.function.IntConsumer;

public class ListenContextFactory {

    public final static ListenContext EMPTY = new ListenContext(0, 0, (__) -> {
    }, (__) -> {
    });

    public static ListenContext readMode(int fd, int eventMask, IntConsumer readCallback) {
        return new ListenContext(fd, eventMask, readCallback, (__) -> {
        });
    }

    public static ListenContext writeMode(int fd, int eventMask, IntConsumer writeCallback) {
        return new ListenContext(fd, eventMask, (__) -> {
        }, writeCallback);
    }


    public static ListenContext merge(ListenContext prime, ListenContext backup) {
        return new ListenContext(
                prime.fd(), prime.eventMask(),
                Objects.requireNonNullElse(prime.readCallback(), backup.readCallback()),
                Objects.requireNonNullElse(prime.writeCallback(), backup.writeCallback()));
    }

}

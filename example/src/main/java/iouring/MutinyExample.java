package iouring;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.MultiSubscribe;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.Cancellable;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.extension.NotEnoughSqeException;

public class MutinyExample {
    private static final String USER_DATA_KEY = "_USER_DATA_KEY_";

    public static void main(String[] args) throws InterruptedException {
        Multi<String> s = Multi.createFrom()
                .emitter((me) -> {
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000L);
                        } catch (Throwable t) {

                        }
                        if(me.isCancelled()) {
                             try {
                            Thread.sleep(2000L);
                        } catch (Throwable t) {

                        }
                        System.out.println("end asyncCancel:"+LocalDateTime.now());
                        }
                        me.emit("args");
                    }).start();
                });

        Cancellable cancellable = s.toUni().onTermination().invoke( () -> {
              System.out.println("onTermination:"+LocalDateTime.now());
        })
                .subscribe().with(System.out::println, Throwable::printStackTrace);
        cancellable.cancel();

    }
}
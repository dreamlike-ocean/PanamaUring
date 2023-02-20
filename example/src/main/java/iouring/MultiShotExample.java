package iouring;


import top.dreamlike.extension.flow.SimplePublisher;
import top.dreamlike.extension.flow.SimpleSubscriber;

import java.util.UUID;

public class MultiShotExample {
    public static void main(String[] args) throws InterruptedException {
        SimplePublisher<UUID> publisher = new SimplePublisher<UUID>(8, () -> {
            System.out.println("canncel");
        });
        SimpleSubscriber<UUID> subscriber = new SimpleSubscriber<>();
        publisher.subscribe(subscriber);
        subscriber.request(8);

        subscriber.setConsumer(s -> System.out.println("get:" + s));


        while (true) {
            Thread.sleep(1000);
            boolean offer = publisher.offer(UUID.randomUUID());
            if (!offer) {
                subscriber.cancel();
                break;
            }
        }
    }
}
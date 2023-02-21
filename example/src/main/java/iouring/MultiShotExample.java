package iouring;


import top.dreamlike.extension.flow.DispatchPublisher;
import top.dreamlike.extension.flow.SimplePublisher;
import top.dreamlike.extension.flow.SimpleSubscriber;

import java.util.UUID;
import java.util.concurrent.Executors;

public class MultiShotExample {
    public static void main(String[] args) throws InterruptedException {
        SimplePublisher<UUID> publisher = new DispatchPublisher<>(8, () -> {
            System.out.println("canncel");
        }, Executors.newSingleThreadExecutor());
        SimpleSubscriber<UUID> subscriber = new SimpleSubscriber<>();
        publisher.subscribe(subscriber);
        subscriber.request(8);

        subscriber.setConsumer(s -> System.out.println("get:" + s + " " + Thread.currentThread()));


        while (true) {
            Thread.sleep(1000);
            boolean offer = publisher.offer(UUID.randomUUID());

        }
    }
}
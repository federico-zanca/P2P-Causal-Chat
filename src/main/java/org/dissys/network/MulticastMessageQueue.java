package org.dissys.network;

import org.dissys.messages.Message;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class MulticastMessageQueue {
    private final ConcurrentLinkedQueue<QueuedMulticastMessage> queue;
    private final AtomicLong sequenceNumber;

    public MulticastMessageQueue() {
        this.queue = new ConcurrentLinkedQueue<>();
        this.sequenceNumber = new AtomicLong(0);
    }

    public void enqueue(Message message, InetAddress group) {
        long seq = sequenceNumber.getAndIncrement();
        queue.offer(new QueuedMulticastMessage(message, group, seq));
    }

    public QueuedMulticastMessage dequeue() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}

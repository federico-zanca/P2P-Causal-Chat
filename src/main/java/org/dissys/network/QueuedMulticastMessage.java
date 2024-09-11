package org.dissys.network;

import org.dissys.messages.Message;

import java.net.InetAddress;

public record QueuedMulticastMessage(Message message, InetAddress group, long sequenceNumber) {
}
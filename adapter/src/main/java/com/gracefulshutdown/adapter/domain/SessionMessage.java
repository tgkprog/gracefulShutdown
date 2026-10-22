package com.gracefulshutdown.adapter.domain;

/**
 * SessionMessage domain model for Adapter service
 */
public class SessionMessage {
    public enum MessageType {
        SESSION_START, SESSION_END
    }

    private MessageType message_type;

    public SessionMessage() {
    }

    public SessionMessage(MessageType message_type) {
        this.message_type = message_type;
    }

    public MessageType getMessage_type() {
        return message_type;
    }

    public void setMessage_type(MessageType message_type) {
        this.message_type = message_type;
    }

    @Override
    public String toString() {
        return "SessionMessage{" +
                "message_type=" + message_type +
                '}';
    }
}

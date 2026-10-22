package com.gracefulshutdown.central.application;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SessionManager {
    private final AtomicReference<SessionState> state = new AtomicReference<>(SessionState.DISCONNECTED);
    private volatile long sessionStartTime = 0;

    public SessionState getState() {
        return state.get();
    }

    public void setState(SessionState newState) {
        state.set(newState);
    }

    public boolean isActive() {
        return state.get() == SessionState.ACTIVE;
    }

    public void startSession() {
        state.set(SessionState.ACTIVE);
        sessionStartTime = System.currentTimeMillis();
    }

    public void requestSessionEnd() {
        state.set(SessionState.SESSION_END_REQUESTED);
    }

    public void disconnect() {
        state.set(SessionState.DISCONNECTED);
        sessionStartTime = 0;
    }

    public long getSessionDuration() {
        if (sessionStartTime > 0) {
            return System.currentTimeMillis() - sessionStartTime;
        }
        return 0;
    }
}

package com.theradio.presence;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveUserRegistry {

    private final Set<Long> activeUsers = ConcurrentHashMap.newKeySet();

    public void addUser(Long userId) {
        activeUsers.add(userId);
    }

    public void removeUser(Long userId) {
        activeUsers.remove(userId);
    }

    public Set<Long> getActiveUsers() {
        return activeUsers;
    }
}

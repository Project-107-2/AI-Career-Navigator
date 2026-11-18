package org.career.navigator.repository;
import org.career.navigator.model.User;

import java.util.HashMap;

public class InMemoryUserRepository implements UserRepository {

    private final HashMap<String, User> users = new HashMap<>();

    @Override
    public void save(User user) {
        users.put(user.email, user);
    }

    @Override
    public User findByEmail(String email) {
        return users.get(email);
    }
}


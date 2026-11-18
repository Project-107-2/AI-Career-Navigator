package org.career.navigator.repository;
import org.career.navigator.model.User;

public interface UserRepository {

    void save(User user);

    User findByEmail(String email);
}


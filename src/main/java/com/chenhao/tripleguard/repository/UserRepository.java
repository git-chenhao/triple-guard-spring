package com.chenhao.tripleguard.repository;

import com.chenhao.tripleguard.entity.User;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class UserRepository {
    private Map<Long, User> userDatabase = new HashMap<>();
    private Long nextId = 1L;
    
    public User findById(Long id) {
        return userDatabase.get(id);
    }
    
    public User findByUsername(String username) {
        for (User user : userDatabase.values()) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }
    
    public List<User> findAll() {
        return new ArrayList<>(userDatabase.values());
    }
    
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(nextId++);
        }
        userDatabase.put(user.getId(), user);
        return user;
    }
    
    public void delete(Long id) {
        userDatabase.remove(id);
    }
}

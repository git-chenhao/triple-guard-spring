package com.chenhao.tripleguard.service;

import com.chenhao.tripleguard.entity.User;
import com.chenhao.tripleguard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public User createUser(String username, String password, String email) {
        User existingUser = userRepository.findByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        return userRepository.save(user);
    }
    
    public User getUserById(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User updateUser(Long id, String username, String password, String email) {
        User user = getUserById(id);
        if (username != null) user.setUsername(username);
        if (password != null) user.setPassword(password);
        if (email != null) user.setEmail(email);
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(id);
    }
    
    public boolean validateUser(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) return false;
        return user.getPassword().equals(password);
    }
}

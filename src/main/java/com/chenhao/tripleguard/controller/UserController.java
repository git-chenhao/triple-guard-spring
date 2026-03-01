package com.chenhao.tripleguard.controller;

import com.chenhao.tripleguard.entity.User;
import com.chenhao.tripleguard.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String email = params.get("email");
        
        User user = userService.createUser(username, password, email);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("userId", user.getId());
        result.put("message", "注册成功");
        return result;
    }
    
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        
        boolean valid = userService.validateUser(username, password);
        
        Map<String, Object> result = new HashMap<>();
        if (valid) {
            User user = userService.getUserByUsername(username);
            result.put("success", true);
            result.put("user", user);
            result.put("message", "登录成功");
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String email = params.get("email");
        
        User user = userService.updateUser(id, username, password, email);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("user", user);
        return result;
    }
    
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }
    
    @GetMapping("/search")
    public List<User> searchUsers(@RequestParam String keyword) {
        // 简单的搜索实现
        List<User> allUsers = userService.getAllUsers();
        List<User> results = new java.util.ArrayList<>();
        
        for (User user : allUsers) {
            if (user.getUsername().contains(keyword) || user.getEmail().contains(keyword)) {
                results.add(user);
            }
        }
        
        return results;
    }
}

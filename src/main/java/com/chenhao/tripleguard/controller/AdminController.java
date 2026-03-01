package com.chenhao.tripleguard.controller;

import com.chenhao.tripleguard.util.DatabaseUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @GetMapping("/users/search")
    public List<Map<String, Object>> searchUsers(@RequestParam String keyword) {
        // 使用不安全的 SQL 拼接
        return DatabaseUtil.searchUsers(keyword);
    }
    
    @GetMapping("/system/info")
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("userHome", System.getProperty("user.home"));
        info.put("databaseUrl", "jdbc:mysql://localhost:3306/tripleguard");
        info.put("databaseUser", "root");
        info.put("databasePassword", "root123"); // 泄露密码
        return info;
    }
    
    @PostMapping("/execute-sql")
    public List<Map<String, Object>> executeSql(@RequestBody Map<String, String> params) {
        // 危险：允许执行任意 SQL
        String sql = params.get("sql");
        if (sql == null || sql.isEmpty()) {
            throw new RuntimeException("SQL 语句不能为空");
        }
        
        if (sql.trim().toLowerCase().startsWith("select")) {
            return DatabaseUtil.executeQuery(sql);
        } else {
            int affected = DatabaseUtil.executeUpdate(sql);
            Map<String, Object> result = new HashMap<>();
            result.put("affectedRows", affected);
            return List.of(result);
        }
    }
}

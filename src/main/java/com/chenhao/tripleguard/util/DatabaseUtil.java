package com.chenhao.tripleguard.util;

import java.sql.*;
import java.util.*;

public class DatabaseUtil {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tripleguard";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root123";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    public static List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
    
    public static int executeUpdate(String sql, Object... params) {
        int affectedRows = 0;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            affectedRows = stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return affectedRows;
    }
    
    public static List<Map<String, Object>> searchUsers(String keyword) {
        String sql = "SELECT * FROM users WHERE username LIKE '%" + keyword + "%' OR email LIKE '%" + keyword + "%'";
        return executeQuery(sql);
    }
}

package com.Southern.book;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootTest
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("✅ 数据库连接成功！");
            System.out.println("数据库URL: " + connection.getMetaData().getURL());
            System.out.println("数据库产品: " + connection.getMetaData().getDatabaseProductName());
            System.out.println("数据库版本: " + connection.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.out.println("❌ 数据库连接失败: " + e.getMessage());
        }
    }

    @Test
    public void testSimpleQuery() {
        try {
            String result = jdbcTemplate.queryForObject("SELECT '数据库连接测试成功'", String.class);
            System.out.println("✅ 简单查询测试: " + result);
        } catch (Exception e) {
            System.out.println("❌ 简单查询失败: " + e.getMessage());
        }
    }

    @Test
    public void testTablesExist() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM users LIMIT 10;",
                    Integer.class
            );
            System.out.println("✅ 数据库中存在 " + count + " 张表");
        } catch (Exception e) {
            System.out.println("❌ 查询表信息失败: " + e.getMessage());
        }
    }
}


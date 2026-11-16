package com.bookon.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DBUtil {
    // 다른 곳에서 DBUtil 객체를 만들 필요가 없으므로 생성자를 막습니다.
    private DBUtil() { }

    // 클래스 경로의 설정을 읽고 새 연결을 반환합니다. 호출한 쪽에서 닫습니다.
    public static Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        try (InputStream in = DBUtil.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new SQLException("클래스 경로에 db.properties가 없습니다.");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new SQLException("DB 설정 파일을 읽지 못했습니다.", e);
        }
        String url = required(properties, "db.url");
        String user = required(properties, "db.user");
        String password = properties.getProperty("db.password");
        if (password == null) {
            throw new SQLException("db.password 설정이 없습니다.");
        }
        try {
            Class.forName(required(properties, "db.driver"));
        } catch (ClassNotFoundException e) {
            throw new SQLException("Oracle JDBC 드라이버를 찾지 못했습니다.", e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    // 필수 설정이 빠졌으면 원인을 알 수 있는 예외를 만듭니다.
    private static String required(Properties properties, String key) throws SQLException {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException(key + " 설정이 없습니다.");
        }
        return value.trim();
    }
}

package com.zettro.java.cloudbox.server;

import java.sql.*;

public class SqlClient {

    private static Connection connection;
    private static Statement statement;

    synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:cloud-box.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static boolean isRegisteredUser(String username, String password) {
        String query = String.format("select * from clients where login='%s' and password='%s'", username, password);
        try (ResultSet set = statement.executeQuery(query)) {
            if (set.next()) return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static void createUser(String username, String password) throws Exception {
        String query = String.format("insert into clients (login, password) values ('%s', '%s')", username, password);
        try {
           statement.execute(query);
        } catch (SQLException e) {
            throw new Exception("Произошла ошибка при создании нового пользователя");
        }
    }
}

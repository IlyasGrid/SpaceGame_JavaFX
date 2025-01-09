package com.example.spaceshootergamejavafx;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/game_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static Connection connection;

    // Méthode pour configurer la connexion à la base de données
    // Initialize the database connection
    public DatabaseManager() {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Successfully connected to the database.");
        } catch ( SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // Méthode pour récupérer la connexion
    public static Connection getConnection() {
        DatabaseManager db = new DatabaseManager();
        return connection;
    }
}

package com.example.spaceshootergamejavafx;

import java.sql.*;

public class UserManager {
 private Connection connection;
 // Initialize uesr
    public UserManager() {

        this.connection = DatabaseManager.getConnection();
    }
    // Ajouter un utilisateur
    public static boolean addUser(User user) {
        String insertSQL = "INSERT INTO users (username, password, score) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setInt(3, 0);
            pstmt.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("L'utilisateur existe déjà.");
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Récupérer un utilisateur par son nom
    public static User getUser(String username) {
        String querySQL = "SELECT * FROM users WHERE username = ?";
        try (
                Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String password = rs.getString("password");
                int score = rs.getInt("score");
                return new User(username, password, score);
            }
        } catch (SQLException e) {
            System.out.println("cconnexion makaynach");
        }
        return null;
    }

    // Mettre à jour le score d'un utilisateur
    public static boolean updateScore(String username, int newScore) {
        String updateSQL = "UPDATE users SET score = ? WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setInt(1, newScore);
            pstmt.setString(2, username);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;

        }
    }

    // Vérifier l'existence d'un utilisateur
    public static boolean userExists(String username) {
        return getUser(username) != null;
    }
}

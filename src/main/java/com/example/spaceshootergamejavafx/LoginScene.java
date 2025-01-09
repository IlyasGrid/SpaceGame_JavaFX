package com.example.spaceshootergamejavafx;

import com.example.spaceshootergamejavafx.SpaceShooter;
import com.example.spaceshootergamejavafx.User;
import com.example.spaceshootergamejavafx.UserManager;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginScene {

    private Stage primaryStage;
    private User currentUser;  // The currently logged-in user

    public LoginScene(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    // Handle the login and signup process
    // Méthode de gestion de la connexion / inscription avec design de gaming
    public void handleLoginSignup() {
        // Créer une VBox pour le formulaire de connexion
        VBox loginLayout = new VBox(20);
        loginLayout.setAlignment(Pos.CENTER);

        // Créer un Label de bienvenue
        Label welcomeLabel = new Label("Bienvenue dans Space Shooter");
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        // Créer un TextField pour le nom d'utilisateur
        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        usernameField.setStyle("-fx-padding: 10px; -fx-font-size: 16px;");

        // Créer un PasswordField pour le mot de passe
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setStyle("-fx-padding: 10px; -fx-font-size: 16px;");

        // Créer des boutons de connexion et d'inscription
        Button loginButton = new Button("Se connecter");
        loginButton.setStyle("-fx-background-color: #1E90FF; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10px;");

        Button signupButton = new Button("S'inscrire");
        signupButton.setStyle("-fx-background-color: #32CD32; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10px;");

        // Créer un Label pour afficher les messages d'erreur
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");

        // Ajouter les éléments dans le layout
        loginLayout.getChildren().addAll(welcomeLabel, usernameField, passwordField, loginButton, signupButton, errorLabel);

        // Créer la scène
        Scene loginScene = new Scene(loginLayout, 400, 400);
        // Si vous voulez un fichier CSS externe pour plus de personnalisation.

        // Afficher la scène
        primaryStage.setTitle("Connexion / Inscription");
        primaryStage.setScene(loginScene);
        primaryStage.show();

        // Action pour le bouton de connexion
        loginButton.setOnAction(event -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Veuillez remplir tous les champs !");
                return;
            }

            // Vérifier l'utilisateur dans la base de données via UserManager
            User user = UserManager.getUser(username);

            if (user == null) {
                errorLabel.setText("Utilisateur non trouvé.");
            } else if (user.getPassword().equals(password)) {
                // Connexion réussie
                this.currentUser = user;  // Affecter l'utilisateur connecté
                errorLabel.setText("Connexion réussie !");
                // Passer à la scène suivante
                // Example: switchToGameScene();
                launchGame(currentUser);
            } else {
                // Mot de passe incorrect
                errorLabel.setText("Mot de passe incorrect !");
            }
        });

        // Action pour le bouton d'inscription
        signupButton.setOnAction(event -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Veuillez remplir tous les champs !");
                return;
            }
            // Vérifier si l'utilisateur existe déjà via UserManager
            boolean userExists = UserManager.userExists(username);

            if (userExists) {
                errorLabel.setText("Cet utilisateur existe déjà !");
            } else {
                // Créer un nouveau compte
                User newUser = new User(username, password);
                if (UserManager.addUser(newUser)) {
                    this.currentUser = newUser;  // Affecter l'utilisateur connecté
                    errorLabel.setText("Inscription réussie !");
                    // Passer à la scène suivante
                    // Example: switchToGameScene();
                    launchGame(currentUser);
                } else {
                    errorLabel.setText("Erreur lors de l'inscription.");
                }
            }
        });
    }
    private void launchGame(User user) {
        // Initialize game with the current user
        SpaceShooter spaceShooter = new SpaceShooter();

        // Transition to the game scene after successful login or sign-up
    }

    public User getCurrentUser() {
        return currentUser;
    }
}

package com.example.spaceshootergamejavafx;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.text.TextAlignment;
import javafx.scene.image.Image;

import javax.print.attribute.standard.Media;

import java.util.*;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
/** Main game class for the Space Shooter game. */
public class SpaceShooter extends Application {
private UserManager userManager;
  /** Width of the game window. */
  public static final int WIDTH = 600;

  /** Height of the game window. */
  public static final int HEIGHT = 800;

  /** Number of lives the player starts with. */
  public static int numLives = 3;

  /** Current score of the player. */
  private int score = 0;

  /** Flag to indicate if a boss enemy exists. */
  private boolean bossExists = false;

  /** Flag to indicate if the game should be reset. */
  private boolean reset = false;

  /** Label to display the player's score. */
  private final Label scoreLabel = new Label("Score: " + score);

  /** Label to display the player's remaining lives. */
  private final Label lifeLabel = new Label("Lives: " + numLives);

  /** List of game objects in the game. */
  private final List<GameObject> gameObjects = new ArrayList<>();

  /** List of new game objects to add to the game. */
  private final List<GameObject> newObjects = new ArrayList<>();

  /** Player object for the game. */
  private Player player = new Player(WIDTH / 2, HEIGHT - 40);

  /** Root pane for the game scene. */
  private Pane root = new Pane();

  /** Game scene for the game. */
  private Scene scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);

  /** Flag to indicate if the level up message has been displayed. */
  private boolean levelUpMessageDisplayed = false;

  /** Flag to indicate if the level up message has been shown. */
  private boolean levelUpShown = false;

  /** Primary stage for the game. */
  private Stage primaryStage;

  /** Flag to indicate if the game is running. */
  private boolean gameRunning = false;
  private User currentUser;

  /** Main method to launch the game. */
  public static void main(String[] args) {
    launch(args);
  }

  /**
   * Starts the game and initializes the game window.
   *
   * @param primaryStage The primary stage for the game
   */
  @Override
  public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;
    primaryStage.setScene(scene);
    primaryStage.setTitle("Space Shooter");
    primaryStage.setResizable(false);

    primaryStage
            .getIcons()
            .add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/player.png"))));

    Canvas canvas = new Canvas(WIDTH, HEIGHT);
    scoreLabel.setTranslateX(10);
    scoreLabel.setTranslateY(10);
    scoreLabel.setTextFill(Color.WHITE);
    scoreLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
    BackgroundImage bgImage = new BackgroundImage(
            new Image(Objects.requireNonNull(getClass().getResource("/img.jpg").toExternalForm())),
            BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.CENTER, new BackgroundSize(1.0, 1.0, true, true, false, false)
    );
    root.setBackground(new Background(bgImage));
    root.getChildren().addAll(canvas, scoreLabel, lifeLabel);

    lifeLabel.setTranslateX(10);
    lifeLabel.setTranslateY(40);
    lifeLabel.setTextFill(Color.WHITE);
    lifeLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));

    GraphicsContext gc = canvas.getGraphicsContext2D();
    gameObjects.add(player);

    Pane menuPane = createMenu();
    Scene menuScene = new Scene(menuPane, WIDTH, HEIGHT);

    primaryStage.setScene(menuScene);
    primaryStage.setTitle("Space Shooter");
    primaryStage.setResizable(false);
    initEventHandlers(scene);

    AnimationTimer gameLoop =
            new AnimationTimer() {
              private long lastEnemySpawned = 0;

              private long lastPowerUpSpawned = 0;

              @Override
              public void handle(long now) {
                if (!gameRunning) return;

                if (reset) {
                  this.start();
                  reset = false;
                }

                gc.setFill(Color.BLACK);
                gc.clearRect(0, 0, WIDTH, HEIGHT);

                if (now - lastEnemySpawned > 1_000_000_000) {
                  spawnEnemy();
                  lastEnemySpawned = now;
                }

                if (now - lastPowerUpSpawned > 10_000_000_000L) {
                  spawnPowerUp();
                  lastPowerUpSpawned = now;
                }

                if (score >= 200 && score % 200 == 0) {
                  boolean bossExists = false;
                  for (GameObject obj : gameObjects) {
                    if (obj instanceof BossEnemy) {
                      bossExists = true;
                      break;
                    }
                  }
                  if (!bossExists) {
                    spawnBossEnemy();
                  }
                }

                checkCollisions();
                checkEnemiesReachingBottom();

                gameObjects.addAll(newObjects);
                newObjects.clear();

                for (GameObject obj : gameObjects) {
                  obj.update();
                  obj.render(gc);
                }

                Iterator<GameObject> iterator = gameObjects.iterator();
                while (iterator.hasNext()) {
                  GameObject obj = iterator.next();
                  if (obj.isDead()) {
                    iterator.remove();
                  }
                }
              }
            };

    gameLoop.start();
    primaryStage.show();
  }

  /** Spawns an enemy at a random x-coordinate at the top of the screen. */
  private void spawnEnemy() {
    Random random = new Random();
    int x = random.nextInt(WIDTH - 50) + 25;

    if (score % 200 == 0 && score > 0 && !bossExists) {
      BossEnemy boss = new BossEnemy(x, -50);
      gameObjects.add(boss);
      showTempMessage("A boss is ahead, watch out!", 75, HEIGHT / 2 - 100, 5);
      bossExists = true; // Ensure we don't spawn multiple bosses
    } else {
      Enemy enemy = new Enemy(x, -40);
      gameObjects.add(enemy);
    }
  }

  /**
   * Checks for collisions between game objects and updates the score and game state accordingly.
   */
  private void checkCollisions() {
    List<Bullet> bullets = new ArrayList<>();
    List<Enemy> enemies = new ArrayList<>();
    List<PowerUp> powerUps = new ArrayList<>();

    for (GameObject obj : gameObjects) {
      if (obj instanceof Bullet) {
        bullets.add((Bullet) obj);
      } else if (obj instanceof Enemy) {
        enemies.add((Enemy) obj);
      } else if (obj instanceof PowerUp) {
        powerUps.add((PowerUp) obj);
      }
    }

    for (Bullet bullet : bullets) {
      for (Enemy enemy : enemies) {
        if (bullet.getBounds().intersects(enemy.getBounds())) {
          bullet.setDead(true);
          if (enemy instanceof BossEnemy) {
            ((BossEnemy) enemy).takeDamage();
            score += 20;
          } else {
            enemy.setDead(true);
            score += 10;
          }
          scoreLabel.setText("Score: " + score);

          if (score % 100 == 0) {
            Enemy.SPEED += 0.9;
          }
        }
      }

      // Check collisions between bullets and power-ups
      for (PowerUp powerUp : powerUps) {
        if (bullet.getBounds().intersects(powerUp.getBounds())) {
          bullet.setDead(true);
          powerUp.setDead(true);
          score += 50;
          scoreLabel.setText("Score: " + score);
        }
      }
    }

    if (score % 100 == 0 && score > 0 && !levelUpShown) {
      showTempMessage("Level Up!", 135, HEIGHT / 2, 2);
      levelUpShown = true;
    } else if (score % 100 != 0) {
      levelUpShown = false;
    }
  }

  /**
   * Checks if any enemies have reached the bottom of the screen and updates the game state
   * accordingly.
   */
  private void checkEnemiesReachingBottom() {
    List<Enemy> enemies = new ArrayList<>();

    for (GameObject obj : gameObjects) {
      if (obj instanceof Enemy) {
        enemies.add((Enemy) obj);
      }
    }

    for (Enemy enemy : enemies) {
      if (enemy.getY() + enemy.getHeight() / 2 >= HEIGHT) {
        enemy.setDead(true);
        enemy.SPEED = enemy.SPEED + 0.4;
        numLives--;
        score -= 10;
        lifeLabel.setText("Lives: " + numLives);
        if (numLives < 0) {
          resetGame();
        }
      }
    }
  }

  /** Shows the losing screen when the player loses all lives. */
  private void showLosingScreen() {
    Pane losingPane = new Pane();
    losingPane.setStyle("-fx-background-color: black;");

    // Game Over Text
    Text gameOverText = new Text("GAME OVER");
    gameOverText.setFont(Font.font("Verdana", FontWeight.BOLD, 40));
    gameOverText.setFill(Color.RED);
    gameOverText.setX((WIDTH - gameOverText.getLayoutBounds().getWidth()) / 2);
    gameOverText.setY(150);

    // Score Display
    if (score < 0) {
      score = 0;
    }

    Text scoreText = new Text("Your Score: " + score);
    scoreText.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
    scoreText.setFill(Color.WHITE);
    scoreText.setX((WIDTH - scoreText.getLayoutBounds().getWidth()) / 2);
    scoreText.setY(250);

    // Try Again Button
    Button tryAgainButton = new Button("Try Again");
    tryAgainButton.setStyle(
        "-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 18; "
            + "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-family: 'Verdana';");
    tryAgainButton.setOnMouseEntered(
        event -> {
          tryAgainButton.setStyle(
              "-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 18; "
                  + "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-family: 'Verdana';");
          tryAgainButton.setEffect(new Glow(0.5));
        });
    tryAgainButton.setOnMouseExited(
        event -> {
          tryAgainButton.setStyle(
              "-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 18; "
                  + "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-family: 'Verdana';");
          tryAgainButton.setEffect(null);
        });
    tryAgainButton.setLayoutX(115);
    tryAgainButton.setLayoutY(350);
    tryAgainButton.setOnAction(event -> restartGame());

    // Exit Button
    Button exitButton = new Button("Exit Game");
    exitButton.setStyle(
        "-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-size: 18; "
            + "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-family: 'Verdana';");
    exitButton.setOnMouseEntered(
        event -> {
          exitButton.setStyle(
              "-fx-background-color: white; -fx-text-fill: red; -fx-font-size: 18; "
                  + "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-family: 'Verdana';");
          exitButton.setEffect(new Glow(0.5));
        });
    exitButton.setOnMouseExited(
        event -> {
          exitButton.setStyle(
              "-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-size: 18; "
                  + "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-family: 'Verdana';");
          exitButton.setEffect(null);
        });
    exitButton.setLayoutX(115);
    exitButton.setLayoutY(450);
    exitButton.setOnAction(event -> System.exit(0));

    losingPane.getChildren().addAll(gameOverText, scoreText, tryAgainButton, exitButton);

    // Create and set the losing screen scene
    Scene losingScene = new Scene(losingPane, WIDTH, HEIGHT);
    primaryStage.setScene(losingScene);
  }

  /** Restarts the game when the player chooses to try again. */
  private void restartGame() {
    gameObjects.clear();
    numLives = 3;
    score = 0;
    lifeLabel.setText("Lives: " + numLives);
    scoreLabel.setText("Score: " + score);
    gameObjects.add(player);
    reset = true;
    gameRunning = true;
    primaryStage.setScene(scene);
  }

  /** Resets the game when the player loses all lives. */
  private void resetGame() {
    gameRunning = false;
    playSound("C:\\Users\\LEGEND\\Desktop\\SpaceGame_JavaFX\\src\\main\\resources\\dead.wav");

    showLosingScreen();
  }

  /**
   * Initializes the event handlers for the game scene.
   *
   * @param scene The game scene to add the event handlers to
   */
  private void initEventHandlers(Scene scene) {
    scene.setOnKeyPressed(
        event -> {
          switch (event.getCode()) {
            case A:
            case LEFT:
              player.setMoveLeft(true);
              break;
            case D:
            case RIGHT:
              player.setMoveRight(true);
              break;
            case S:
            case DOWN:
              player.setMoveBackward(true);
              break;
            case W:
            case UP:
              player.setMoveForward(true);
              break;
            case SPACE:
              player.shoot(newObjects);
              playSound("C:\\Users\\LEGEND\\Desktop\\SpaceGame_JavaFX\\src\\main\\resources\\shoot.wav");
              break;
          }
        });

    scene.setOnKeyReleased(
        event -> {
          switch (event.getCode()) {
            case A:
            case LEFT:
              player.setMoveLeft(false);
              break;
            case D:
            case RIGHT:
              player.setMoveRight(false);
              break;
            case S:
            case DOWN:
              player.setMoveBackward(false);
              break;
            case W:
            case UP:
              player.setMoveForward(false);
              break;
          }
        });
  }

  /** Spawns a power-up at a random x-coordinate at the top of the screen. */
  private void spawnPowerUp() {
    Random random = new Random();
    int x = random.nextInt(WIDTH - PowerUp.WIDTH) + PowerUp.WIDTH / 2;
    PowerUp powerUp = new PowerUp(x, -PowerUp.HEIGHT / 2);
    gameObjects.add(powerUp);
  }

  /** Spawns a boss enemy at the top of the screen. */
  private void spawnBossEnemy() {
    if (gameObjects.stream().noneMatch(obj -> obj instanceof BossEnemy)) {
      BossEnemy bossEnemy = new BossEnemy(WIDTH / 2, -40);
      gameObjects.add(bossEnemy);
      EnemyBullet enB = new EnemyBullet(WIDTH / 2, -40);
      gameObjects.add(enB);
      bossEnemy.shoot(gameObjects);
    }
  }

  /**
   * Creates the main menu for the game.
   *
   * @return The main menu pane
   */

  private Pane createMenu() {
        Pane menuPane = new Pane();
        menuPane.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 80%, #0f0f0f, #1e1e1e);" +
                "-fx-border-color: #00ffcc; -fx-border-width: 5; -fx-border-radius: 10;"
        );

        Text welcomeText = new Text("\u2588\u2588\u2588 Welcome to \u2588\u2588\u2588\nSPACE SHOOTER!");
        welcomeText.setFont(Font.font("Consolas", FontWeight.EXTRA_BOLD, 36));
        welcomeText.setFill(Color.LIMEGREEN);
        welcomeText.setEffect(new Glow(0.8));
        welcomeText.setTextAlignment(TextAlignment.CENTER);
        welcomeText.setX(WIDTH / 2 - 200);
        welcomeText.setY(100);

        // Bouton de connexion / inscription
        Button loginButton = createStyledButton("LOGIN/SIGNUP", 100);
        loginButton.setOnMouseEntered(e -> loginButton.setEffect(new Glow(0.6)));
        loginButton.setOnMouseExited(e -> loginButton.setEffect(null));
        //loginButton.setOnAction(event -> handleLoginSignup());

        // Bouton de démarrage du jeu
        Button startButton = createStyledButton("START", 200);
        startButton.setOnMouseEntered(e -> startButton.setEffect(new Glow(0.6)));
        startButton.setOnMouseExited(e -> startButton.setEffect(null));
        startButton.setOnAction(event -> startGame());

        // Bouton des instructions
        Button instructionsButton = createStyledButton("INSTRUCTIONS", 300);
        instructionsButton.setOnMouseEntered(e -> instructionsButton.setEffect(new Glow(0.6)));
        instructionsButton.setOnMouseExited(e -> instructionsButton.setEffect(null));
        instructionsButton.setOnAction(event -> showInstructions());

        // Bouton quitter
        Button quitButton = createStyledButton("QUIT", 400);
        quitButton.setOnMouseEntered(e -> quitButton.setEffect(new Glow(0.6)));
        quitButton.setOnMouseExited(e -> quitButton.setEffect(null));
        quitButton.setOnAction(event -> System.exit(0));

        // Conteneur des boutons
        VBox buttonsContainer = new VBox(20);
        buttonsContainer.setLayoutX(WIDTH / 2 - 100);
        buttonsContainer.setLayoutY(200);
        buttonsContainer.getChildren().addAll(loginButton, startButton, instructionsButton, quitButton);

        menuPane.getChildren().addAll(welcomeText, buttonsContainer);

        return menuPane;
    }



  // Helper method for creating styled buttons
  private Button createStyledButton(String text, int yPosition) {
    Button button = new Button(text);
    button.setPrefWidth(200);
    button.setPrefHeight(50);
    button.setLayoutY(yPosition);
    button.setStyle(
            "-fx-background-color: #222222; -fx-text-fill: #ffffff; " +
                    "-fx-font-family: 'Consolas'; -fx-font-size: 18; -fx-border-color: #00ffcc; " +
                    "-fx-border-radius: 10; -fx-background-radius: 10;"
    );
    return button;
  }


  /**
   * Creates a styled button with a gradient background and hover effects.
   *
   * @param text The text to display on the button
   * @param y The y-coordinate of the button
   * @return The styled button
   */
  private Button createStyledButton(String text, double y) {
    Button button = new Button(text);
    button.setStyle(
        "-fx-background-color: linear-gradient(to right, #6a11cb, #2575fc);"
            + "-fx-text-fill: white;"
            + "-fx-font-size: 18;"
            + "-fx-font-weight: bold;"
            + "-fx-padding: 10 20;"
            + "-fx-border-radius: 20;"
            + "-fx-background-radius: 20;"
            + "-fx-border-color: #ffffff;"
            + "-fx-border-width: 2;"
            + "-fx-font-family: 'Verdana';");
    button.setOnMouseEntered(
        event -> {
          button.setStyle(
              "-fx-background-color: linear-gradient(to right, #2575fc, #6a11cb);"
                  + "-fx-text-fill: yellow;"
                  + "-fx-font-size: 18;"
                  + "-fx-font-weight: bold;"
                  + "-fx-padding: 10 20;"
                  + "-fx-border-radius: 20;"
                  + "-fx-background-radius: 20;"
                  + "-fx-border-color: yellow;"
                  + "-fx-border-width: 2;"
                  + "-fx-font-family: 'Verdana';");
          button.setEffect(new Glow(0.5));
        });
    button.setOnMouseExited(
        event -> {
          button.setStyle(
              "-fx-background-color: linear-gradient(to right, #6a11cb, #2575fc);"
                  + "-fx-text-fill: white;"
                  + "-fx-font-size: 18;"
                  + "-fx-font-weight: bold;"
                  + "-fx-padding: 10 20;"
                  + "-fx-border-radius: 20;"
                  + "-fx-background-radius: 20;"
                  + "-fx-border-color: #ffffff;"
                  + "-fx-border-width: 2;"
                  + "-fx-font-family: 'Verdana';");
          button.setEffect(null);
        });
    return button;
  }


  /** Shows the instructions for the game. */
  private void showInstructions() {
    Alert instructionsAlert = new Alert(AlertType.INFORMATION);
    instructionsAlert.setTitle("\uD83D\uDEA8 Instructions - Space Shooter \uD83D\uDEA8");
    instructionsAlert.setHeaderText(null);

    // Customizing dialog style
    DialogPane dialogPane = instructionsAlert.getDialogPane();
    dialogPane.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #0f2027, #203a43, #2c5364); " +
                    "-fx-border-color: #00ffcc; -fx-border-width: 2; -fx-border-radius: 10; " +
                    "-fx-background-radius: 10;"
    );

    // Setting the text style
    Label contentLabel = new Label(
            "\u2714 Use the A, W, S, and D keys or the arrow keys to move your spaceship.\n" +
                    "\u2714 Press SPACE to shoot bullets and destroy the enemies.\n" +
                    "\u2714 If an enemy reaches the bottom of the screen, you lose a life.\n" +
                    "\u2714 The game resets if you lose all lives.\n" +
                    "\u2714 Collect power-ups to increase your score.\n" +
                    "\u2714 Defeat the boss enemy to level up and increase the difficulty.\n\n" +
                    "\uD83C\uDFC6 Good luck and have fun! \uD83C\uDFC6"
    );
    contentLabel.setStyle("-fx-text-fill: #00ffcc; -fx-font-family: 'Consolas'; -fx-font-size: 16;");
    contentLabel.setWrapText(true);

    // Adding styled content to the alert
    instructionsAlert.getDialogPane().setContent(contentLabel);

    instructionsAlert.showAndWait();
  }


  /**
   * Shows a temporary message on the screen for a specified duration.
   *
   * @param message The message to display
   * @param x The x-coordinate of the message
   * @param y The y-coordinate of the message
   * @param duration The duration to display the message
   */
  private void showTempMessage(String message, double x, double y, double duration) {
    Text tempMessage = new Text(message);
    tempMessage.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
    tempMessage.setFill(Color.RED);
    tempMessage.setX(x);
    tempMessage.setY(y);
    root.getChildren().add(tempMessage);

    PauseTransition pause = new PauseTransition(Duration.seconds(duration));
    pause.setOnFinished(event -> root.getChildren().remove(tempMessage));
    pause.play();
  }
  private void playSound(String soundFilePath) {

    new Thread(() -> {
      try {
        // Replace with a valid WAV file path
        File soundFile = new File(soundFilePath);
        if (!soundFile.exists()) {
          System.err.println("File does not exist: " + soundFilePath);
          return;
        }

        AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
        Clip clip = AudioSystem.getClip();
        clip.open(audioStream);

        clip.start();
      } catch (UnsupportedAudioFileException e) {
        System.err.println("Unsupported audio file: " + soundFilePath);
      } catch (IOException e) {
        System.err.println("Error loading audio file: " + e.getMessage());
      } catch (LineUnavailableException e) {
        System.err.println("Audio line unavailable: " + e.getMessage());
      }
    }).start();

  }

  /** Starts the game when the player clicks the start button. */
  private void startGame() {
    gameRunning = true;

    // Ensure you are using a WAV file for this method to work
    primaryStage.setScene(scene);
  }

}

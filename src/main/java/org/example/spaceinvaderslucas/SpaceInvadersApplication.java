package org.example.spaceinvaderslucas;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SpaceInvadersApplication extends Application {

    private final static int APP_HEIGHT = 600;
    private final static int APP_WIDTH = 800;
    private static final int INVULNERABILITY_DURATION = 2000; // 2 seconds of invulnerability after being hit
    private static final int GAME_OVER_BOUNDARY = APP_HEIGHT - 150; // Y position where enemies cause game over
    private static final double ALIEN_LASER_SPEED = 200.0; // Positive because moving downwards
    private static final Group root = new Group();
    private static final double LASER_SPEED = -400.0; // Negative because moving upwards
    private static final int MAX_ACTIVE_LASERS = 15; // Limit simultaneous lasers
    private final List<AlienLaser> alienLasers = new ArrayList<>();
    private final GameObject[][] ufos = new GameObject[5][11]; // 5 rows, 11 columns of UFOs
    private final GameObject[][] enemiesMoved = new GameObject[5][11];
    private final int SPACE = 40;
    private final List<GameObject> activeLasers = new ArrayList<>();
    private final List<Explosion> activeExplosions = new ArrayList<>();
    private double elapsedTime;
    private Long startNanoTime;
    private int totalEnemies;
    private int coordinateY = 80;
    private int coordinateX = APP_WIDTH / 3 - (40 * 3);
    private boolean SHIFTING_RIGHT, PLAYER_SHOT, GAME_IS_WON, GAME_OVER;
    private double maxShiftLeft, maxShiftRight;
    private boolean leftKeyPressed = false;
    private boolean rightKeyPressed = false;
    private int playerScore = 0;
    private int playerLives = 3;
    private boolean playerInvulnerable = false;
    private long invulnerabilityTimer = 0;
    private double alienShootTimer = 0;
    private GameObject[][] currentEnemies;
    private double time = 0.40;
    private double enemyMoveInterval = 1.5;
    private double enemyMoveDistance = 15.0;
    private GameObject airplane;


    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {

        stage.setTitle("Space Invaders!");
        stage.setResizable(false);

        Canvas gameCanvas = new Canvas(APP_WIDTH, APP_HEIGHT);
        GraphicsContext gc2d = gameCanvas.getGraphicsContext2D();

        Scene gameScene = new Scene(root);
        gameScene.setFill(Color.BLACK);

        root.getChildren().add(gameCanvas);

        airplane = createAirplane();
        airplane.render(gc2d);

        spawnEnemies(gc2d);
        setMovedEnemies();
        updateCurrentEnemies();

        gameScene.setOnKeyPressed(event -> {
            if (GAME_OVER) {
                if (event.getCode() == KeyCode.ENTER) {
                    restartGame(gc2d);
                }
            } else if (event.getCode() == KeyCode.LEFT) {
                leftKeyPressed = true;
                updateAirplaneMovement(airplane);
            } else if (event.getCode() == KeyCode.RIGHT) {
                rightKeyPressed = true;
                updateAirplaneMovement(airplane);
            } else if (event.getCode() == KeyCode.SPACE) {
                shoot();
            }
        });

        gameScene.setOnKeyReleased(event -> {
            if (GAME_OVER) return;

            if (event.getCode() == KeyCode.LEFT) {
                leftKeyPressed = false;
                updateAirplaneMovement(airplane);
            } else if (event.getCode() == KeyCode.RIGHT) {
                rightKeyPressed = false;
                updateAirplaneMovement(airplane);
            }
        });

        AnimationTimer timer = new AnimationTimer() {

            @Override
            public void handle(long now) {
                if (startNanoTime == null) {
                    startNanoTime = System.nanoTime();
                }
                elapsedTime = (now - startNanoTime) / 1000000000.0;
                startNanoTime = now;

                if (playerInvulnerable) {
                    if (System.currentTimeMillis() - invulnerabilityTimer > INVULNERABILITY_DURATION) {
                        playerInvulnerable = false;
                    }
                }

                // Update alien shooting timer
                alienShootTimer += elapsedTime;

                // Clear the entire screen first
                gc2d.clearRect(0, 0, APP_WIDTH, APP_HEIGHT);

                displayScore(gc2d);

                if (GAME_OVER) {
                    displayGameEndMessage(gc2d);
                    displayScore(gc2d);
                    return;
                }

                gc2d.clearRect(0, 0, APP_WIDTH, APP_HEIGHT);

                if (airplane != null) {
                    if (airplane.getX() < 50) {
                        airplane.setPosition(50.0 + 1, airplane.getY());
                        airplane.setVelocity(0.0, 0.0);
                    } else if (airplane.getX() > APP_WIDTH - 100) {
                        airplane.setPosition(airplane.getX() - 1, airplane.getY());
                        airplane.setVelocity(0.0, 0.0);
                    }
                }

                if (!playerInvulnerable || (System.currentTimeMillis() / 100) % 2 == 0) {
                    airplane.render(gc2d);
                }
                airplane.updatePosition(gc2d, elapsedTime);

                updateLasers(gc2d, elapsedTime);

                updateExplosions(gc2d, elapsedTime);

                handleAlienShooting();

                updateAlienLasers(gc2d, elapsedTime);

                time += elapsedTime;

                // Calculate current enemy move interval based on score
                // As score increases, interval decreases (making enemies faster)
                // Minimum interval is enemyMoveInterval/5 (5x speed)
                double currentMoveInterval = Math.max(enemyMoveInterval / 5, enemyMoveInterval - (playerScore / 200.0 * 0.5));

                // Calculate current enemy move distance based on score
                // As score increases, move distance increases
                double currentMoveDistance = enemyMoveDistance + (playerScore / 150.0 * 5.0);
                currentMoveDistance = Math.min(currentMoveDistance, 40.0); // Cap at reasonable maximum

                getMaxShiftSpace();
                if (time >= currentMoveInterval) {
                    int moveDistance = (int) Math.round(currentMoveDistance);

                    if (SHIFTING_RIGHT) {
                        if (maxShiftRight < 640) {
                            coordinateX += moveDistance;
                        } else {
                            coordinateY += moveDistance;
                            SHIFTING_RIGHT = false;
                        }
                    } else {
                        if (maxShiftLeft > 80) {
                            coordinateX -= moveDistance;
                        } else {
                            coordinateY += moveDistance;
                            SHIFTING_RIGHT = true;
                        }
                    }
                    updateCurrentEnemies();
                    time = 0;

                    // Periodically verify arrays consistency
                    verifyEnemyArraysConsistency();
                }
                animateEnemies(gc2d);

                displayScore(gc2d);
            }
        };

        timer.start();

        stage.setScene(gameScene);


        stage.show();
    }

    private GameObject createAirplane() {
        GameObject airplane = new GameObject();
        airplane.setImageFromFilename("/images/airplane.png");
        airplane.setPosition(APP_WIDTH / 2.0 - 20, APP_HEIGHT - 80.0);
        return airplane;
    }

    private void moveAirplaneLeft(GameObject airplane) {
        airplane.setVelocity(-250.0, 0.0);
    }

    private void moveAirplaneRight(GameObject airplane) {
        airplane.setVelocity(250.0, 0.0);
    }

    /**
     * Stops the airplane movement by setting its velocity to zero
     */
    private void stopAirplane(GameObject airplane) {
        airplane.setVelocity(0.0, 0.0);
    }

    /**
     * Updates airplane movement based on which keys are currently pressed
     */
    private void updateAirplaneMovement(GameObject airplane) {
        // If both or neither keys are pressed, stop movement
        if ((leftKeyPressed && rightKeyPressed) || (!leftKeyPressed && !rightKeyPressed)) {
            stopAirplane(airplane);
        } else if (leftKeyPressed) {
            moveAirplaneLeft(airplane);
        } else {
            moveAirplaneRight(airplane);
        }
    }

    private void spawnEnemies(GraphicsContext gc) {
        for (int y = 80, i = 0; y < APP_HEIGHT / 2 + SPACE && i < 5; y += SPACE, i++) {
            for (int x = APP_WIDTH / 3 - (SPACE * 3), j = 0; x < 660 && j < 11; x += SPACE, j++) {
                if (y < 90) {
                    ufos[i][j] = spawnAlien(x, y, "/images/small_invader_a.png");
                    gc.drawImage(ufos[i][j].getImage(), x, y);
                } else if (y < 200) {
                    ufos[i][j] = spawnAlien(x, y, "/images/medium_invader_a.png");
                    gc.drawImage(ufos[i][j].getImage(), x, y);
                } else {
                    ufos[i][j] = spawnAlien(x, y, "/images/large_invader_a.png");
                    gc.drawImage(ufos[i][j].getImage(), x, y);
                }
                totalEnemies++;
            }
        }
    }

    private GameObject spawnAlien(double x, double y, String imagePath) {
        GameObject smallAlien = new GameObject();
        smallAlien.setSmallImageFromFilename(imagePath);
        smallAlien.setPosition(x, y);
        return smallAlien;
    }

    private void setMovedEnemies() {
        for (int y = 80, i = 0; y < APP_HEIGHT / 2 + SPACE && i < 5; y += SPACE, i++) {
            for (int x = APP_WIDTH / 3 - (SPACE * 3), j = 0; x < 660 && j < 11; x += SPACE, j++) {
                // Only create moved enemies where there's a matching ufo
                if (ufos[i][j] != null) {
                    if (y < 90) {
                        enemiesMoved[i][j] = spawnAlien(x, y, "/images/small_invader_b.png");
                    } else if (y < 200) {
                        enemiesMoved[i][j] = spawnAlien(x, y, "/images/medium_invader_b.png");
                    } else {
                        enemiesMoved[i][j] = spawnAlien(x, y, "/images/large_invader_b.png");
                    }

                    // Copy position from ufos if already set
                    if (ufos[i][j].getPosition() != null) {
                        enemiesMoved[i][j].setPosition(
                                ufos[i][j].getPosition().getKey(),
                                ufos[i][j].getPosition().getValue()
                        );
                    }
                } else {
                    // Ensure consistency - if no ufo, no moved enemy
                    enemiesMoved[i][j] = null;
                }
            }
        }
    }

    private void updateCurrentEnemies() {
        // Update current enemies array reference
        GameObject[][] previousEnemies = currentEnemies;
        currentEnemies = currentEnemies == ufos ? enemiesMoved : ufos;

        // Copy position data from previous array to maintain consistent positions
        if (previousEnemies != null) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 11; j++) {
                    if (previousEnemies[i][j] != null && currentEnemies[i][j] != null) {
                        currentEnemies[i][j].setPosition(
                                previousEnemies[i][j].getX(),
                                previousEnemies[i][j].getY()
                        );
                    }
                }
            }
        }
    }

    private void animateEnemies(GraphicsContext gc) {
        boolean enemyReachedBottom = false;

        for (int y = coordinateY, i = 0; y < APP_HEIGHT - 100 && i < 5; y += SPACE, i++) {
            for (int x = coordinateX, j = 0; x < 700 && j < 11; x += SPACE, j++) {
                if (currentEnemies[i][j] != null) {
                    // Update the position in the game object
                    currentEnemies[i][j].setPosition(x * 1.0, y * 1.0);

                    // Check if any enemy has reached the bottom boundary
                    if (currentEnemies[i][j].getY() >= GAME_OVER_BOUNDARY) {
                        enemyReachedBottom = true;
                    }

                    // Render from the game object's current position rather than using x,y directly
                    gc.drawImage(currentEnemies[i][j].getImage(),
                            currentEnemies[i][j].getX(),
                            currentEnemies[i][j].getY());
                }
            }
        }

        // Check if enemies reached the bottom
        if (enemyReachedBottom && !GAME_OVER) {
            GAME_OVER = true;
            GAME_IS_WON = false;
        }
    }

    private void getMaxShiftSpace() {
        maxShiftLeft = 0.00;
        maxShiftRight = 0.00;
        //looking at the far left side
        for (GameObject[] enemy : currentEnemies) {
            for (int j = 0; j < currentEnemies[0].length; j++) {
                if (enemy[j] != null) {
                    if (maxShiftLeft > 0.00) {
                        maxShiftLeft = Math.min(maxShiftLeft, enemy[j].getX());
                    } else {
                        maxShiftLeft = enemy[j].getX();
                    }
                    break;
                }
            }
        }
        //looking at the far right side
        for (GameObject[] currentEnemy : currentEnemies) {
            for (int j = currentEnemies[0].length - 1; j >= 0; j--) {
                if (currentEnemy[j] != null) {
                    if (maxShiftRight > 0.00) {
                        maxShiftRight = Math.max(maxShiftRight, currentEnemy[j].getX());
                    } else {
                        maxShiftRight = currentEnemy[j].getX();
                    }
                    break;
                }
            }
        }
    }

    private void shoot() {
        if (activeLasers.size() >= MAX_ACTIVE_LASERS || PLAYER_SHOT) {
            return; // Limit the number of simultaneous lasers
        }

        GameObject laser = new GameObject();
        laser.setImageFromFilename("/images/missile.png");

        double airplaneX = airplane.getX();
        double airplaneY = airplane.getY();
        laser.setPosition(airplaneX + 20, airplaneY - 10);

        // Set vertical velocity (moving upward)
        laser.setVelocity(0.0, LASER_SPEED);

        activeLasers.add(laser);
        PLAYER_SHOT = true;

        // Automatically reset PLAYER_SHOT after a short delay to allow shooting again
        new Thread(() -> {
            try {
                Thread.sleep(50);
                PLAYER_SHOT = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateLasers(GraphicsContext gc, double elapsedTime) {

        // Update player lasers
        Iterator<GameObject> iterator = activeLasers.iterator();
        while (iterator.hasNext()) {
            GameObject laser = iterator.next();

            // Update laser position
            laser.updatePosition(gc, elapsedTime);

            // Check if laser is off screen
            if (laser.getY() < 0) {
                iterator.remove();
                PLAYER_SHOT = false;
                continue;
            }

            // Check for collisions with enemies
            boolean collisionDetected = false;
            for (int i = 0; i < currentEnemies.length && !collisionDetected; i++) {
                for (int j = 0; j < currentEnemies[i].length && !collisionDetected; j++) {
                    if (currentEnemies[i][j] != null && checkCollision(laser, currentEnemies[i][j])) {
                        // Get enemy position before removing it
                        double enemyX = currentEnemies[i][j].getX();
                        double enemyY = currentEnemies[i][j].getY();

                        // Create explosion at enemy position
                        createExplosion(enemyX + 15, enemyY + 15); // Center of the enemy

                        // Enemy hit - remove from both arrays to maintain consistency
                        currentEnemies[i][j] = null;

                        // Synchronize the other array (whichever one is not current)
                        if (currentEnemies == ufos) {
                            enemiesMoved[i][j] = null;
                        } else {
                            ufos[i][j] = null;
                        }

                        iterator.remove();
                        PLAYER_SHOT = false;
                        totalEnemies--;

                        // Update score based on enemy type (height determines type)
                        if (enemyY < 90) {
                            playerScore += 30; // Small invaders (top row) worth more
                        } else if (enemyY < 200) {
                            playerScore += 20; // Medium invaders (middle rows)
                        } else {
                            playerScore += 10; // Large invaders (bottom rows)
                        }

                        // Check if all enemies are defeated
                        if (totalEnemies <= 0) {
                            GAME_OVER = true;
                            GAME_IS_WON = true;
                        }

                        collisionDetected = true;
                    }
                }
            }

            // If no collision was detected and laser is still on screen, render it
            if (!collisionDetected) {
                laser.render(gc);
            }
        }
    }

    /**
     * Updates alien lasers and checks for collisions with the player
     */
    private void updateAlienLasers(GraphicsContext gc, double elapsedTime) {
        if (alienLasers.isEmpty()) {
            return; // Nothing to update
        }

        System.out.println("Updating " + alienLasers.size() + " alien lasers");
        Iterator<AlienLaser> iterator = alienLasers.iterator();
        while (iterator.hasNext()) {
            AlienLaser laser = iterator.next();

            // Update laser position
            laser.update(elapsedTime);

            // Check if laser is off screen
            if (laser.isOffScreen(APP_HEIGHT)) {
                System.out.println("Alien laser went off screen, removing");
                iterator.remove();
                continue;
            }

            // Check for collision with player
            if (!playerInvulnerable && laser.checkCollision(airplane)) {
                // Player hit - lose a life
                playerLives--;
                iterator.remove();

                // Create explosion at player position
                double playerX = airplane.getX();
                double playerY = airplane.getY();
                createExplosion(playerX + 20, playerY + 20); // Center of the player

                // Make player temporarily invulnerable
                playerInvulnerable = true;
                invulnerabilityTimer = System.currentTimeMillis();

                // Check if game over (no lives left)
                if (playerLives <= 0) {
                    GAME_OVER = true;
                    GAME_IS_WON = false;
                }

                continue;
            }

            // Render laser
            laser.render(gc);
        }
    }

    private boolean checkCollision(GameObject laser, GameObject enemy) {
        if (enemy == null) return false;

        double laserX = laser.getX();
        double laserY = laser.getY();
        double enemyX = enemy.getX();
        double enemyY = enemy.getY();

        // Adjust these values based on your sprite sizes
        int laserWidth = 4;   // Typical laser width
        int laserHeight = 15; // Typical laser height
        int enemyWidth = 30;  // Typical enemy width
        int enemyHeight = 30; // Typical enemy height

        return laserX < enemyX + enemyWidth &&
                laserX + laserWidth > enemyX &&
                laserY < enemyY + enemyHeight &&
                laserY + laserHeight > enemyY;
    }

    /**
     * Creates an explosion effect at the specified position
     */
    private void createExplosion(double x, double y) {
        Explosion explosion = new Explosion(x, y);
        activeExplosions.add(explosion);
    }

    /**
     * Debug helper method to verify that both enemy arrays are consistent
     * This helps ensure that when an enemy is destroyed in one array, it's also removed from the other.
     */
    private void verifyEnemyArraysConsistency() {
        int ufosCount = 0;
        int movedEnemiesCount = 0;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 11; j++) {
                if (ufos[i][j] != null) ufosCount++;
                if (enemiesMoved[i][j] != null) movedEnemiesCount++;

                // Check for inconsistency
                if ((ufos[i][j] == null && enemiesMoved[i][j] != null) ||
                        (ufos[i][j] != null && enemiesMoved[i][j] == null)) {
                    System.err.println("Enemy arrays inconsistency detected at [" + i + "][" + j + "]");
                    // Synchronize by making both null or both populated
                    if (ufos[i][j] == null) {
                        enemiesMoved[i][j] = null;
                    } else {
                        // This case shouldn't happen if properly synchronized
                        ufos[i][j] = null;
                    }
                }
            }
        }

        if (ufosCount != movedEnemiesCount) {
            System.err.println("Enemy count mismatch: ufos=" + ufosCount + ", enemiesMoved=" + movedEnemiesCount);
        }
    }

    /**
     * Updates and renders all active explosions
     */
    private void updateExplosions(GraphicsContext gc, double elapsedTime) {
        Iterator<Explosion> iterator = activeExplosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.update(elapsedTime);

            if (explosion.isActive()) {
                explosion.render(gc);
            } else {
                iterator.remove(); // Remove inactive explosions
            }
        }
    }

    /**
     * Handles alien shooting logic
     */
    private void handleAlienShooting() {
        // Use a more aggressive shooting interval to ensure aliens are firing
        double adjustedInterval = 0.5; // Fire every half second for testing

        System.out.println("Alien shoot timer: " + alienShootTimer + ", adjusted interval: " + adjustedInterval);

        // Check if it's time to shoot
        if (alienShootTimer >= adjustedInterval) {
            System.out.println("Time to shoot!");
            alienShootTimer = 0;
            fireAlienLaser();
        }
    }

    /**
     * Creates and fires a laser from a random alien
     */
    private void fireAlienLaser() {
        System.out.println("Attempting to fire alien laser");
        // Find all active aliens
        List<GameObject> activeAliens = new ArrayList<>();
        for (int i = 0; i < currentEnemies.length; i++) {
            for (int j = 0; j < currentEnemies[i].length; j++) {
                if (currentEnemies[i][j] != null) {
                    activeAliens.add(currentEnemies[i][j]);
                }
            }
        }

        // If no aliens remain, return
        if (activeAliens.isEmpty()) {
            System.out.println("No active aliens to shoot");
            return;
        }

        System.out.println("Found " + activeAliens.size() + " active aliens");

        // Select a random alien to shoot
        GameObject shooter = activeAliens.get((int) (Math.random() * activeAliens.size()));

        // Position laser at the bottom-center of the alien
        double alienX = shooter.getX();
        double alienY = shooter.getY();

        // Create a new AlienLaser with proper position and speed
        AlienLaser laser = new AlienLaser(alienX + 15, alienY + 30, ALIEN_LASER_SPEED);

        // Add to alien lasers list
        alienLasers.add(laser);
        System.out.println("Added laser to alienLasers list, size now: " + alienLasers.size());
    }

    /**
     * Displays the current score on the screen
     */
    private void displayScore(GraphicsContext gc) {
        // Calculate current speed factor with updated formula for max 5x
        double currentInterval = Math.max(enemyMoveInterval / 5, enemyMoveInterval - (playerScore / 200.0 * 0.5));
        double speedFactor = enemyMoveInterval / currentInterval;

        // Draw dark background for score area
        gc.setFill(Color.BLACK);
        gc.fillRect(10, 10, 380, 30);

        // Draw score text
        gc.setFill(Color.GREEN);
        gc.setFont(javafx.scene.text.Font.font("Arial", 20));
        gc.fillText("SCORE: " + playerScore, 20, 30);

        // Show speed multiplier with color changing based on speed
        // Speed color shifts from yellow to red as it increases
        double colorIntensity = Math.min(1.0, (speedFactor - 1.0) / 4.0); // 0 to 1 based on speed 1x to 5x
        gc.setFill(Color.color(1.0, 1.0 - colorIntensity, 0)); // Yellow to red
        gc.fillText("SPEED: " + String.format("%.1f", speedFactor) + "x", 160, 30);

        // Display player lives
        gc.setFill(playerInvulnerable ? Color.YELLOW : Color.RED); // Yellow during invulnerability
        gc.fillText("LIVES: " + playerLives, 280, 30);
    }

    /**
     * Displays game end message based on win/lose condition
     */
    private void displayGameEndMessage(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 40));

        String message = GAME_IS_WON ? "YOU DA MAN!" : "YOU LOSE!";

        // Calculate text position for center alignment
        double textWidth = message.length() * 20; // Rough estimate of text width
        double textX = (APP_WIDTH - textWidth) / 2;

        // Create a semi-transparent background for the text
        gc.setGlobalAlpha(0.7);
        gc.setFill(Color.BLACK);
        gc.fillRect(textX - 20, APP_HEIGHT / 2.0 - 40, textWidth + 40, 80);

        // Draw the text
        gc.setGlobalAlpha(1.0);
        gc.setFill(GAME_IS_WON ? Color.GREEN : Color.RED);
        gc.fillText(message, textX, APP_HEIGHT / 2.0 + 10);

        // Add restart instructions
        gc.setFont(javafx.scene.text.Font.font("Arial", 20));
        gc.setFill(Color.WHITE);
        gc.fillText("Press ENTER to restart", textX, APP_HEIGHT / 2.0 + 50);
    }

    /**
     * Restarts the game by resetting all necessary game state variables
     */
    private void restartGame(GraphicsContext gc) {
        // Reset game state variables
        GAME_OVER = false;
        GAME_IS_WON = false;
        playerScore = 0;
        playerLives = 3;
        playerInvulnerable = false;
        totalEnemies = 0;
        coordinateY = 80;
        coordinateX = APP_WIDTH / 3 - (40 * 3);
        SHIFTING_RIGHT = false;
        time = 0.40;
        alienShootTimer = 0;
        // Reset key state variables
        leftKeyPressed = false;
        rightKeyPressed = false;
        // Reset enemy movement speed to default values
        enemyMoveInterval = 1.5;
        enemyMoveDistance = 15.0;

        // Clear active lasers and explosions
        activeLasers.clear();
        alienLasers.clear(); // This is now a list of AlienLaser objects
        activeExplosions.clear();

        // Reset player position
        airplane.setPosition(APP_WIDTH / 2.0 - 20, APP_HEIGHT - 80.0);
        airplane.setVelocity(0.0, 0.0);

        // Respawn enemies
        spawnEnemies(gc);
        setMovedEnemies();
        updateCurrentEnemies();
    }
}
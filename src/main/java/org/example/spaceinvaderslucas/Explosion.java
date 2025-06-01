package org.example.spaceinvaderslucas;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.Objects;

public class Explosion {
    private final Image explosionImage;
    private final double x;
    private final double y;
    private final double duration;
    private double elapsedTime;
    private boolean active;

    public Explosion(double x, double y) {
        this.x = x;
        this.y = y;
        this.explosionImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/explosion.png")));
        this.duration = 0.5; // Duration in seconds
        this.elapsedTime = 0;
        this.active = true;
    }

    public void update(double deltaTime) {
        elapsedTime += deltaTime;
        if (elapsedTime >= duration) {
            active = false;
        }
    }

    public void render(GraphicsContext gc) {
        if (active) {

            double scale = Math.min(1.0, elapsedTime / (duration * 0.5));
            double size = explosionImage.getWidth() * scale;

            // Draw explosion with fade-out effect
            double opacity = 1.0 - (elapsedTime / duration);
            gc.setGlobalAlpha(opacity);

            // Center the explosion on the impact point
            gc.drawImage(explosionImage, 
                         x - (size/2), 
                         y - (size/2), 
                         size, 
                         size);

            gc.setGlobalAlpha(1.0); // Reset opacity
            // Sound is now played once in the constructor
        }
    }

    public boolean isActive() {
        return active;
    }
}

package org.example.spaceinvaderslucas;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * A visual-only explosion effect that doesn't depend on sound playback
 */
public class ExplosionEffect {
    private double x;
    private double y;
    private double radius;
    private double maxRadius;
    private double duration;
    private double elapsedTime;
    private boolean active;
    private Color color;
    private Image explosionImage;

    public ExplosionEffect(double x, double y) {
        this.x = x;
        this.y = y;
        this.radius = 5.0;
        this.maxRadius = 30.0;
        this.duration = 0.5; // Duration in seconds
        this.elapsedTime = 0;
        this.active = true;
        this.color = Color.ORANGE;  // Start with orange

        // Try to load the explosion image, but have a fallback
        try {
            this.explosionImage = new Image(getClass().getResourceAsStream("/images/explosion.png"));
        } catch (Exception e) {
            System.err.println("Could not load explosion image");
            this.explosionImage = null;
        }
    }

    public void update(double deltaTime) {
        elapsedTime += deltaTime;

        // Update the explosion radius
        double progress = Math.min(1.0, elapsedTime / duration);
        radius = progress * maxRadius;

        // Transition color from orange to red to transparent
        double redComponent = 1.0; // always full red
        double greenComponent = Math.max(0, 0.65 - (progress * 0.65)); // orange to red
        double blueComponent = 0.0; // no blue
        double alphaComponent = 1.0 - Math.pow(progress, 2); // fade out, more rapidly near end

        color = Color.color(redComponent, greenComponent, blueComponent, alphaComponent);

        // Check if animation is complete
        if (elapsedTime >= duration) {
            active = false;
        }
    }

    public void render(GraphicsContext gc) {
        if (!active) return;

        if (explosionImage != null) {
            // If we have the image, use it with scaling
            double scale = Math.min(1.0, elapsedTime / (duration * 0.5));
            double size = explosionImage.getWidth() * scale;

            // Apply fade effect
            double opacity = 1.0 - (elapsedTime / duration);
            gc.setGlobalAlpha(opacity);

            // Center the explosion
            gc.drawImage(explosionImage, x - (size/2), y - (size/2), size, size);
            gc.setGlobalAlpha(1.0); // Reset opacity
        } else {
            // Use a simple circle explosion with color effects if image isn't available
            gc.setFill(color);
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

            // Add an inner circle for effect
            gc.setFill(Color.YELLOW);
            gc.fillOval(x - radius * 0.6, y - radius * 0.6, radius * 1.2, radius * 1.2);
        }
    }

    public boolean isActive() {
        return active;
    }
}

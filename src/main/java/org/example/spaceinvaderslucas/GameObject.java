package org.example.spaceinvaderslucas;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.util.Pair;

public class GameObject {

    private Image image;

    private Pair<Double, Double> position;
    private Pair<Double, Double> velocity;

    public GameObject() {
        this.position = new Pair<>(0.0, 0.0);
        this.velocity = new Pair<>(0.0, 0.0);
    }


    public void setImage(Image image) {
        this.image = image;
    }

    public void setImageFromFilename(String filename) {
        Image img = new Image(getClass().getResource(filename).toExternalForm());
        setImage(img);
    }

    public void setSmallImageFromFilename(String filename) {
        Image img = new Image(getClass().getResource(filename).toExternalForm());
        Image smallImg = new Image(getClass().getResource(filename).toExternalForm(), img.getWidth() / 3, img.getHeight() / 3, true, false);
        setImage(smallImg);
    }

    public void render(GraphicsContext graphicsContext) {
        graphicsContext.drawImage(image, position.getKey(), position.getValue());
    }

    public void setPosition(Double x, Double y) {
        this.position = new Pair<>(x, y);
    }

    public Pair<Double, Double> getPosition() {
        return position;
    }

    public void setVelocity(Double x, Double y) {
        this.velocity = new Pair<>(x, y);
    }

    public void updatePosition(GraphicsContext graphicsContext, double elapsedTime) {
        System.out.println("Updating position with elapsed time: " + elapsedTime);
        position = new Pair<>(this.getPosition().getKey() + (this.velocity.getKey() * elapsedTime), this.position.getValue() +
                (this.velocity.getValue() * elapsedTime));
        System.out.println("New position: " + position.getKey() + ", " + position.getValue());
    }

    public Image getImage() {
        return this.image;
    }

    public double getX(){
        return this.getPosition().getKey();
    }

    public double getY(){
        return this.getPosition().getValue();
    }
}

module org.example.spaceinvaderslucas {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.jdi;
    requires javafx.media;


    opens org.example.spaceinvaderslucas to javafx.fxml;
    exports org.example.spaceinvaderslucas;
}
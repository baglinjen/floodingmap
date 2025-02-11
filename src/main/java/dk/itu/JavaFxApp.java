package dk.itu;

import dk.itu.drawing.components.MapView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JavaFxApp extends Application {

    @Override
    public void start(Stage stage) {
        // Create new Scene
        Scene mapScene = new Scene(new MapView());

        stage.setScene(mapScene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
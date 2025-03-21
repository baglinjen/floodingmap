package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;

public class DebugComponent extends BorderPane {
    private DebugComponent() {
        super();
        setPadding(new Insets(12));
        setStyle("-fx-background-color: white;");
    }

    public DebugComponent(State state) {
        this();
        setLeft(new WaterSliderComponent(state));
        setCenter(new MouseCoordinatesComponent(state));
        setRight(new ActionsComponent(state));
    }
}

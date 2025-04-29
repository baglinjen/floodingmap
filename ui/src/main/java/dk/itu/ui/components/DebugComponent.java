package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class DebugComponent extends BorderPane {
    private DebugComponent() {
        super();
        setPadding(new Insets(12));
        setStyle("-fx-background-color: white;");
    }

    public DebugComponent(State state) {
        this();
        var mouseReactiveTextContainer = new HBox(new MouseCoordinatesComponent(state), new SelectedHeightCurveHeightComponent(state));
        mouseReactiveTextContainer.setSpacing(4);
        mouseReactiveTextContainer.setAlignment(Pos.CENTER);
        setLeft(new WaterSliderComponent(state));
        setCenter(mouseReactiveTextContainer);
        setRight(new ActionsComponent(state));
    }
}

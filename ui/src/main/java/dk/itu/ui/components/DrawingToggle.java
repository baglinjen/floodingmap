package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.ToggleButton;

public class DrawingToggle extends ToggleButton {
    public DrawingToggle(State state) {
        super("Toggle Draw GeoJson");
        setOnAction(_ -> state.toggleShouldDrawGeoJson());
    }
}

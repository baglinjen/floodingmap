package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.Label;

public class MouseCoordinatesComponent extends Label {
    public MouseCoordinatesComponent(State state) {
        super();
        var mouseLonLat = state.getMouseLonLat();
        setText(String.format("Y: %.4f X: %.4f", mouseLonLat.getX(), mouseLonLat.getY()));
        state.addOnMouseMovedListener((lonLat) -> {
            setText(String.format("Y: %.4f X: %.4f", lonLat.getX(), lonLat.getY()));
        });
    }
}

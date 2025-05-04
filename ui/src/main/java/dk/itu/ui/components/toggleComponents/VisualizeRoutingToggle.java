package dk.itu.ui.components.toggleComponents;

import dk.itu.ui.State;
import javafx.scene.control.ToggleButton;

public class VisualizeRoutingToggle extends ToggleButton {
    public VisualizeRoutingToggle(State state) {
        super("Visualize routing search");
        setOnAction(_ -> state.getRoutingConfiguration().toggleShouldVisualize());
    }
}

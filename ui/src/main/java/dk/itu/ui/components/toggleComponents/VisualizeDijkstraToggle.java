package dk.itu.ui.components.toggleComponents;

import dk.itu.ui.State;
import javafx.scene.control.ToggleButton;

public class VisualizeDijkstraToggle extends ToggleButton {
    public VisualizeDijkstraToggle(State state) {
        super("Visualize Dijkstra");
        setOnAction(_ -> state.getDijkstraConfiguration().toggleShouldVisualize());
    }
}

package dk.itu.ui.components.toggleComponents;

import dk.itu.ui.State;
import javafx.scene.control.ToggleButton;

public class NearestNeighbourToggle extends ToggleButton {
    public NearestNeighbourToggle(State state) {
        super("Show nearest neighbour");
        setSelected(state.getShowNearestNeighbour());
        setOnAction(_ -> {
            state.setShowNearestNeighbour(!state.getShowNearestNeighbour());
            setSelected(state.getShowNearestNeighbour());
        });
    }
}

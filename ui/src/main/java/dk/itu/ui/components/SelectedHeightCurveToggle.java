package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.ToggleButton;

public class SelectedHeightCurveToggle extends ToggleButton {
    public SelectedHeightCurveToggle(State state) {
        super("Show selected height curve");
        setSelected(state.getShowSelected());
        setOnAction(_ -> {
            if (state.getShowSelected()) {
                if (state.getHcSelected() != null) {
                    state.getHcSelected().setSelected(false);
                    state.setHcSelected(null);
                }
            }
            state.setShowSelected(!state.getShowSelected());
        });
    }
}

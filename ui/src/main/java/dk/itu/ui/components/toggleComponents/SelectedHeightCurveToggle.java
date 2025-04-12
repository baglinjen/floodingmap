package dk.itu.ui.components.toggleComponents;

import dk.itu.ui.State;
import javafx.scene.control.ToggleButton;

public class SelectedHeightCurveToggle extends ToggleButton {
    public SelectedHeightCurveToggle(State state) {
        super("Show selected height curve");
        setSelected(state.getShowSelectedHeightCurve());
        setOnAction(_ -> {
            if (state.getShowSelectedHeightCurve()) {
                if (state.getHcSelected() != null) {
                    state.getHcSelected().setSelected(false);
                    state.setHcSelected(null);
                }
            }
            state.setShowSelectedHeightCurve(!state.getShowSelectedHeightCurve());
        });
    }
}

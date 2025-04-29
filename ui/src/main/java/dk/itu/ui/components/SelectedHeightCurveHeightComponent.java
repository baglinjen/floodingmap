package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.Label;

public class SelectedHeightCurveHeightComponent extends Label {
    public SelectedHeightCurveHeightComponent(State state) {
        super();
        setVisible(false);
        state.addOnMouseMovedListener(_ -> {
            if (state.getHcSelected() != null) {
                if (!isVisible()) setVisible(true);
                setText("HC Height: " + state.getHcSelected().getHeight());
            }
        });
    }
}

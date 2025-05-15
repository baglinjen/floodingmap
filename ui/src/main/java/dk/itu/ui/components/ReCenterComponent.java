package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.Button;

public class ReCenterComponent extends Button {
    public ReCenterComponent(State state) {
        super("Re-center Map");
        setOnAction(_ -> state.resetWindowBounds());
    }
}
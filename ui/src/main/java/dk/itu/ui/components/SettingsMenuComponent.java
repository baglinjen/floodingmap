package dk.itu.ui.components;

import dk.itu.ui.State;
import dk.itu.ui.components.toggleComponents.DrawingToggle;
import dk.itu.ui.components.toggleComponents.NearestNeighbourToggle;
import dk.itu.ui.components.toggleComponents.SelectedHeightCurveToggle;
import dk.itu.ui.components.toggleComponents.VisualizeRoutingToggle;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;

public class SettingsMenuComponent extends MenuButton {
    public SettingsMenuComponent(State state){
        super();
        setText("Settings");

        getItems().addAll(
                new CustomMenuItem(new DrawingToggle(state)),
                new CustomMenuItem(new NearestNeighbourToggle(state)),
                new CustomMenuItem(new SelectedHeightCurveToggle(state)),
                new CustomMenuItem(new VisualizeRoutingToggle(state))
        );
    }
}
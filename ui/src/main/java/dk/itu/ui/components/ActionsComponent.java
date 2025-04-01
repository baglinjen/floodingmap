package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.layout.HBox;

public class ActionsComponent extends HBox {
    public ActionsComponent(State state) {
        super(10);
        getChildren().addAll(
                new ReCenterComponent(state),
                new SelectedHeightCurveToggle(state),
                new DrawingToggle(state),
                new DataResetComponent(),
                new DataLoadComponent()
        );
    }
}

package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.layout.HBox;

public class ActionsComponent extends HBox {
    public ActionsComponent(State state) {
        super(12);
        getChildren().addAll(
                new ReCenterComponent(state),
                new SelectedHeightCurveToggle(state),
                new NearestNeighbourToggle(state),
                new DrawingToggle(state),
//                new DijkstraToggle(state),
                new CalculateRouteButton(state),
                new DataResetComponent(state),
                new DataLoadComponent(state),
                new LoadHeightCurvesComponent(state)
        );
    }
}

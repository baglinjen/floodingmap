package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class CalculateRouteButton extends Button {
    public CalculateRouteButton(State state) {
        super("Calculate Route");
        setOnAction(_ -> {
            if (state.getRoutingConfiguration().getStartNode() == null || state.getRoutingConfiguration().getEndNode() == null) {
                displayAlert("Both start and end for route must be selected");
            } else {
                state.getRoutingConfiguration().calculateRoute(state.isWithDb());
            }
        });
    }
    private void displayAlert(String errorMessage){
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Something went wrong");
        alert.setHeaderText("An exception was thrown when attempting to calculate routing");
        alert.setContentText("Message was: " + errorMessage);

        alert.showAndWait();
    }
}

package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class MapMenu extends ContextMenu {
    public MapMenu(State state) {
        MenuItem routeStart = new MenuItem("Set Route Start");
        routeStart.setOnAction(_ -> {
            var neighbour = state.getNearestNeighbour();
            if (neighbour != null) {
                state.getDijkstraConfiguration().setStartNode(state.getNearestNeighbour().getSelectedOsmElement());
            }
        });
        MenuItem routeEnd = new MenuItem("Set Route End");
        routeEnd.setOnAction(_ -> {
            var neighbour = state.getNearestNeighbour();
            if (neighbour != null) {
                state.getDijkstraConfiguration().setEndNode(state.getNearestNeighbour().getSelectedOsmElement());
            }
        });
        MenuItem calculateRoute = new MenuItem("Calculate Route");
        calculateRoute.setOnAction(_ -> {
            if (state.getDijkstraConfiguration().getStartNode() == null || state.getDijkstraConfiguration().getEndNode() == null) {
                displayAlert("Both start and end for route must be selected");
            } else {
                state.getDijkstraConfiguration().calculateRoute(state.isWithDb());
            }
        });
        getItems().addAll(routeStart, routeEnd, calculateRoute);
    }

    private void displayAlert(String errorMessage){
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Something went wrong");
        alert.setHeaderText("An exception was thrown when attempting to calculate routing");
        alert.setContentText("Message was: " + errorMessage);

        alert.showAndWait();
    }
}

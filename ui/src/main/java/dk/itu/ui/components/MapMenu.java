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
        getItems().addAll(routeStart, routeEnd, createRoutingButton(state, false), createRoutingButton(state, true));
    }

    private MenuItem createRoutingButton(State state, boolean isAStar){
        MenuItem item = new MenuItem("Route with " + (isAStar ? "A-Star" : "Dijkstra"));
        item.setOnAction(_ -> {
            if (state.getDijkstraConfiguration().getStartNode() == null || state.getDijkstraConfiguration().getEndNode() == null){
                displayAlert("Both start and end for route must be selected");
            } else{
                state.getDijkstraConfiguration().setIsAStar(isAStar);
                state.getDijkstraConfiguration().calculateRoute(state.isWithDb());
            }
        });

        return item;
    }

    private void displayAlert(String errorMessage){
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Something went wrong");
        alert.setHeaderText("An exception was thrown when attempting to calculate routing");
        alert.setContentText("Message was: " + errorMessage);

        alert.showAndWait();
    }
}
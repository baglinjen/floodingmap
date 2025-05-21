package dk.itu.ui.components;

import dk.itu.data.enums.RoutingType;
import dk.itu.ui.State;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class MapMenu extends ContextMenu {
    public MapMenu(State state) {
        try{
            MenuItem routeStart = new MenuItem("Set Route Start");
            routeStart.setOnAction(_ -> {
                var neighbour = state.getNearestNeighbour();
                if (neighbour != null) {
                    state.getRoutingConfiguration().setStartNode(state.getNearestNeighbour().getSelectedOsmElement());
                }
            });
            MenuItem routeEnd = new MenuItem("Set Route End");
            routeEnd.setOnAction(_ -> {
                var neighbour = state.getNearestNeighbour();
                if (neighbour != null) {
                    state.getRoutingConfiguration().setEndNode(state.getNearestNeighbour().getSelectedOsmElement());
                }
            });

        MenuItem cancelRoutingButton = new MenuItem("Cancel routing");
        cancelRoutingButton.setOnAction(_ -> {
            state.getRoutingConfiguration().cancelRouteCalculation();
        });

            getItems().addAll(
                    routeStart,
                    routeEnd,
                    createRoutingButton(state, RoutingType.Dijkstra),
                    createRoutingButton(state, RoutingType.AStar),
                    createRoutingButton(state, RoutingType.AStarBidirectional),
                    cancelRoutingButton
            );
        } catch(RuntimeException ex){
            displayAlert(ex.getMessage());
        }

    }

    private MenuItem createRoutingButton(State state, RoutingType routeType) throws RuntimeException{
        var displayMethod = switch (routeType) {
            case Dijkstra -> "Dijkstra";
            case AStar -> "A-Star";
            case AStarBidirectional -> "A-Star bidirectional";
        };

        MenuItem item = new MenuItem("Route with: " + displayMethod);

        item.setOnAction(_ -> {
            if (state.getRoutingConfiguration().getStartNode() == null || state.getRoutingConfiguration().getEndNode() == null){
                displayAlert("Both start and end for route must be selected");
            } else{
                try{
                    state.getRoutingConfiguration().setRoutingMethod(routeType);
                    state.getRoutingConfiguration().calculateRoute();
                } catch(RuntimeException ex){
                    displayAlert(ex.getMessage());
                }
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
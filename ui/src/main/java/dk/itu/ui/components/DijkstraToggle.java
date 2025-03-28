package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.*;

public class DijkstraToggle extends MenuButton {
    public DijkstraToggle(State state){
        super("Create Routing");

        var fromField = new TextField();
        fromField.setPromptText("From ID");
        var fromItem = new CustomMenuItem(fromField);
        fromItem.setHideOnClick(false);

        var toField = new TextField();
        toField.setPromptText("To ID");
        var toItem = new CustomMenuItem(toField);
        toItem.setHideOnClick(false);

        var button = new Button("Calculate");
        button.setOnAction(e -> {
            try{
                state.getDijkstraConfiguration().setStartNodeId(fromField.getText().trim());
                state.getDijkstraConfiguration().setEndNodeId(toField.getText().trim());
                state.getDijkstraConfiguration().calculateRoute();
            } catch(NumberFormatException ex){
                DisplayAlert("Invalid node ID");
            } catch(Exception ex){
                DisplayAlert(ex.getMessage());
            }

        });

        var buttonItem = new CustomMenuItem(button);

        this.getItems().addAll(fromItem, toItem, buttonItem);
    }

    private void DisplayAlert(String errorMessage){
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Something went wrong");
        alert.setHeaderText("An exception was thrown when attempting to calculate routing");
        alert.setContentText("Message was: " + errorMessage);

        alert.showAndWait();
    }
}
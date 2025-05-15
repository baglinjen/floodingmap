package dk.itu.ui.components;

import dk.itu.data.services.Services;
import dk.itu.ui.State;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.awt.geom.Point2D;

public class MouseEventOverlayComponent extends BorderPane {

    public MouseEventOverlayComponent(State state) {
        super();

        //Create text labels
        var toggleSelectedHC = new Label("H: Toggle Selected HC      ");
        var toggleNN = new Label("N : Toggle NN      ");
        var toggleDrawHC = new Label("D: Toggle Draw HCs      ");
        var toggleVisualizeRoute = new Label("R: Toggle Visualize Route      ");
        var toggleDrawBB = new Label("B: Toggle Draw Bounding Boxes      ");
        var generalLabel = new Label("S: Set Start      E: Set End      C: Calculate Route      L: Load HC");

        // Set event handlers
        setOnMousePressed(event -> {state.mouseMoved(event.getX(), event.getY());});
        setOnMouseDragged(event -> {
            double dx = event.getX() - state.getMouseX(), dy = event.getY() - state.getMouseY();
            state.getSuperAffine().prependTranslation(dx, dy);
            state.mouseMoved(event.getX(), event.getY());
        });
        setOnScroll(event -> {
            double zoom = event.getDeltaY() > 0 ? 1.05 : 1/1.05;
            state.getSuperAffine()
                    .prependTranslation(-event.getX(), -event.getY())
                    .prependScale(zoom, zoom)
                    .prependTranslation(event.getX(), event.getY());
        });
        setOnMouseMoved(event -> {
            state.mouseMoved(event.getX(), event.getY());
            var mousePos = state.getMouseLonLat();
            Services.withServices(services -> {
                if (state.getShowSelectedHeightCurve()) selectHeightCurve(state, services, mousePos);
                selectNearestNeighbour(state, services, mousePos);
            });
        });
        setOnMouseClicked(_ -> {
            var mousePos = state.getMouseLonLat();
            Services.withServices(services -> {
                selectNearestNeighbour(state, services, mousePos);
            });
        });
        setOnKeyTyped(event -> {
            switch (event.getCharacter().toUpperCase()) {
                case "H":
                    if (state.getShowSelectedHeightCurve()) {
                        if (state.getHcSelected() != null) {
                            state.getHcSelected().setUnselected();
                            state.setHcSelected(null);
                        }
                    }
                    state.setShowSelectedHeightCurve(!state.getShowSelectedHeightCurve());
                    toggleSelectedHC.setTextFill(state.getShowSelectedHeightCurve() ? Color.GREEN : Color.BLACK);

                    break;
                case "N":
                    state.setShowNearestNeighbour(!state.getShowNearestNeighbour());

                    toggleNN.setTextFill(state.getShowNearestNeighbour() ? Color.GREEN : Color.BLACK);
                    break;
                case "S":
                    if (state.getNearestNeighbour() == null) {
                        Services.withServices(services -> {
                            selectNearestNeighbour(state, services, state.getMouseLonLat());
                        });
                    }
                    if (state.getNearestNeighbour() != null) state.getRoutingConfiguration().setStartNode(state.getNearestNeighbour().getSelectedOsmElement());
                    break;
                case "E":
                    if (state.getNearestNeighbour() == null) {
                        Services.withServices(services -> {
                            selectNearestNeighbour(state, services, state.getMouseLonLat());
                        });
                    }
                    if (state.getNearestNeighbour() != null) state.getRoutingConfiguration().setEndNode(state.getNearestNeighbour().getSelectedOsmElement());
                    break;
                case "C":
                    try {
                        state.getRoutingConfiguration().calculateRoute(state.isWithDb());
                    } catch (Exception e) {
                        var alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Something went wrong");
                        alert.setHeaderText("An exception was thrown when attempting to calculate routing");
                        alert.setContentText(e.getMessage());
                        alert.showAndWait();
                    }
                    break;
                case "D":
                    state.toggleShouldDrawGeoJson();

                    toggleDrawHC.setTextFill(state.shouldDrawGeoJson() ? Color.GREEN : Color.BLACK);
                    break;
                case "L":
                    Services.withServices(services -> {
                        double[] wb = state.getWindowBounds();
                        services.getHeightCurveService().loadGmlData(wb[0], wb[1], wb[2], wb[3]);
                        state.updateMinMaxWaterLevels(services);
                    });
                    break;
                case "B":
                    state.toggleShouldDrawBoundingBoxes();

                    toggleDrawBB.setTextFill(state.shouldDrawBoundingBox() ? Color.GREEN : Color.BLACK);
                    break;
                case "R":
                    state.getRoutingConfiguration().toggleShouldVisualize();

                    toggleVisualizeRoute.setTextFill(state.getRoutingConfiguration().getShouldVisualize() ? Color.GREEN : Color.BLACK);
                    break;
            }
        });
        var contextMenu = new MapMenu(state);
        setOnContextMenuRequested(e -> contextMenu.show(this, e.getSceneX(), e.getSceneY()));

        //Add debug component with shortcuts
        var keyLabelContainer = new HBox();
        keyLabelContainer.getChildren().addAll(toggleSelectedHC, toggleNN, toggleDrawHC, toggleVisualizeRoute, toggleDrawBB, generalLabel);

        keyLabelContainer.setAlignment(Pos.CENTER);
        keyLabelContainer.setBackground(Background.fill(Paint.valueOf("#ffffff")));
        keyLabelContainer.setPadding(new Insets(5, 0, 5, 0));

        setTop(keyLabelContainer);
        setBottom(new DebugComponent(state));
    }

    private static void selectHeightCurve(State state, Services services, Point2D.Double mousePos) {
        if (!state.getShowSelectedHeightCurve()) return;
        var hc = services.getHeightCurveService().getHeightCurveForPoint(mousePos.getX(), mousePos.getY());

        if (state.getHcSelected() != null) {
            state.getHcSelected().setUnselected();
        }
        hc.setSelected();
        state.setHcSelected(hc);
    }

    private static void selectNearestNeighbour(State state, Services services, Point2D.Double mousePos) {
        if (!state.getShowNearestNeighbour()) return;
        state.setSelectedOsmElement(
                services.getOsmService(state.isWithDb()).getNearestTraversableOsmNode(mousePos.getX(), mousePos.getY())
        );
    }
}
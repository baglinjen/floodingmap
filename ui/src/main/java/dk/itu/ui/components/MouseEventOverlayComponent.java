package dk.itu.ui.components;

import dk.itu.data.services.Services;
import dk.itu.ui.State;
import javafx.scene.layout.BorderPane;

import java.awt.geom.Point2D;

public class MouseEventOverlayComponent extends BorderPane {

    public MouseEventOverlayComponent(State state) {
        super();

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
        var contextMenu = new MapMenu(state);
        setOnContextMenuRequested(e -> contextMenu.show(this, e.getSceneX(), e.getSceneY()));

        // Add debug component
        setBottom(new DebugComponent(state));
    }

    private static void selectHeightCurve(State state, Services services, Point2D.Double mousePos) {
        if (!state.getShowSelectedHeightCurve()) return;
        var curveTree = services.getGeoJsonService().getCurveTree();
        if (curveTree == null) return;
        var hc = services.getGeoJsonService().getCurveTree().getHeightCurveForPoint(mousePos.getX(), mousePos.getY());
        if (state.getHcSelected() != null) {
            state.getHcSelected().setSelected(false);
            hc.setSelected(true);
        }
        state.setHcSelected(hc);
    }

    private static void selectNearestNeighbour(State state, Services services, Point2D.Double mousePos) {
        if (!state.getShowNearestNeighbour()) return;
        state.setSelectedOsmElement(
                services.getOsmService(state.isWithDb()).getNearestTraversableOsmNode(mousePos.getX(), mousePos.getY())
        );
    }
}
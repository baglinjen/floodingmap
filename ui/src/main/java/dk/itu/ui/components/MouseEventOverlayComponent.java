package dk.itu.ui.components;

import dk.itu.data.services.Services;
import dk.itu.ui.State;
import javafx.scene.layout.BorderPane;

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
            if (!state.getShowSelected()) return;
            var mousePos = state.getMouseLonLat();
            Services.withServices(services -> {
                var hc = services.getGeoJsonService().getCurveTree().getHeightCurveForPoint(mousePos.getX(), mousePos.getY());
                if (state.getHcSelected() != null) {
                    state.getHcSelected().setSelected(false);
                    hc.setSelected(true);
                }
                state.setHcSelected(hc);
            });
        });

        // Add debug component
        setBottom(new DebugComponent(state));
    }
}
package dk.itu.ui.components;

import dk.itu.data.services.Services;
import dk.itu.ui.State;
import javafx.scene.control.Button;

public class LoadHeightCurvesComponent extends Button {
    public LoadHeightCurvesComponent(State state) {
        super("Load Height Curves");
        setOnAction(_ -> Services.withServices(services -> {
            var windowBounds = state.getWindowBounds();
            services.getHeightCurveService().loadGmlData(windowBounds[0], windowBounds[1], windowBounds[2], windowBounds[3]);
            state.updateMinMaxWaterLevels(services);
        }));
    }
}
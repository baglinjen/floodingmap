package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.Slider;

public class WaterSliderComponent extends Slider {
    public WaterSliderComponent(State state) {
        super(Math.max(state.getMinWaterLevel(), 0), state.getMaxWaterLevel(), state.getWaterLevel());
        setPrefWidth(350);
        setShowTickMarks(true);
        setShowTickLabels(true);
        setSnapToTicks(true);
        setMajorTickUnit(0.5);
        setBlockIncrement(0.5);

        valueProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue.floatValue() != newValue.floatValue()) {
                state.setWaterLevel(newValue.floatValue());
            }
        });
    }
}
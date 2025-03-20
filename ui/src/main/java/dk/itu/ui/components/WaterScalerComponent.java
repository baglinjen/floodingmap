package dk.itu.ui.components;

import dk.itu.ui.State;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

public class WaterScalerComponent extends VBox {
    public WaterScalerComponent(State state) {
        Slider slider = new Slider(Math.max(state.getMinWaterLevel(), 0), state.getMaxWaterLevel(), state.getWaterLevel()); // Min, Max, Initial
        slider.setPrefWidth(350);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setSnapToTicks(true);
        slider.setMajorTickUnit(0.5);
        slider.setBlockIncrement(0.5);

        slider.valueProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue.floatValue() != newValue.floatValue()) {
                state.setWaterLevel(newValue.floatValue());
            }
        });

        getChildren().add(slider);
    }
}
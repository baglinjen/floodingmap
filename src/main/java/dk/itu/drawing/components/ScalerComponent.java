package dk.itu.drawing.components;

import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

import java.util.concurrent.atomic.AtomicReference;

public class ScalerComponent extends VBox {
    public ScalerComponent(AtomicReference<Float> waterLevel) {
        Slider slider = new Slider(0, 25, waterLevel.get()); // Min, Max, Initial
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setSnapToTicks(true);
        slider.setMajorTickUnit(5);
        slider.setBlockIncrement(5);

        slider.valueProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue.floatValue() != newValue.floatValue()) {
                waterLevel.set(newValue.floatValue());
            }
        });

        getChildren().add(slider);
    }
}
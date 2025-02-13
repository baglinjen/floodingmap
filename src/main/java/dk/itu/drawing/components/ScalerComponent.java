package dk.itu.drawing.components;

import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

public class ScalerComponent extends VBox {
    private ScalerListener listener;

    public ScalerComponent() {
        Slider slider = new Slider(1, 30, 0); // Min, Max, Initial
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setMajorTickUnit(1);
        slider.setBlockIncrement(1);
        slider.setSnapToTicks(true);

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (listener != null) {
                listener.onScaleChanged(newValue.intValue());
            }
        });

        getChildren().add(slider);
    }

    public void setScalerListener(ScalerListener listener) {
        this.listener = listener;
    }

    public interface ScalerListener {
        void onScaleChanged(int newValue);
    }
}

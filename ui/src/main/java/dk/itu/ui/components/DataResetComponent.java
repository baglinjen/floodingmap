package dk.itu.ui.components;

import dk.itu.data.services.Services;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

public class DataResetComponent extends SplitMenuButton {
    private ResetOption option = null;
    public DataResetComponent() {
        super();
        setText("Select option to reset");
        getItems().addAll(
                createMenuItem("OSM", ResetOption.OSM)
//                createMenuItem("GEOJSON", ResetOption.GEOJSON)
        );
        setOnAction(_ -> {
            if (option == null) return;
            Services.withServices(services -> {
                if (option == ResetOption.OSM) {
                    services.getOsmService().clearAll();
                }
            });
        });
    }

    private MenuItem createMenuItem(String text, ResetOption optionType) {
        MenuItem osmMenuItem = new MenuItem(text);
        osmMenuItem.setOnAction(_ -> {
            option = optionType;
            setText("Reset " + text);
        });
        return osmMenuItem;
    }

    private enum ResetOption {
        OSM,
        GEOJSON
    }
}

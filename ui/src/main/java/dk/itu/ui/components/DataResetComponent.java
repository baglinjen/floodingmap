package dk.itu.ui.components;

import dk.itu.data.services.Services;
import dk.itu.ui.State;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

public class DataResetComponent extends SplitMenuButton {
    private ResetOption option = ResetOption.OSM;
    public DataResetComponent(State state) {
        super();
        setText("Reset OSM");
        getItems().addAll(
                createMenuItem("OSM", ResetOption.OSM)
//                createMenuItem("GEOJSON", ResetOption.GEOJSON)
        );
        setOnAction(_ -> {
            Services.withServices(services -> {
                if (option == ResetOption.OSM) {
                    services.getOsmService(state.isWithDb()).clearAll();
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

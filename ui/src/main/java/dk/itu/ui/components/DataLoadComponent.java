package dk.itu.ui.components;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.services.Services;
import dk.itu.ui.State;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

public class DataLoadComponent extends SplitMenuButton {
    private String selectedFile = null;
    private Thread task = null;
    public DataLoadComponent(State state) {
        super();
        setText("Select data file");
        getItems().addAll(
                CommonConfiguration
                        .getInstance()
                        .getDataFiles()
                        .parallelStream()
                        .map(this::createMenuItem)
                        .toList()
        );
        setOnAction(_ -> {
            if (task != null || selectedFile == null || (!selectedFile.endsWith(".osm") && !selectedFile.endsWith(".geojson"))) {
                return;
            }
            Services.withServices(services -> {
                if (selectedFile.endsWith(".osm")) {
                    setDisable(true);
                    setText("Loading OSM file");
                    services.getOsmService(state.isWithDb()).loadOsmData(selectedFile);
                    setDisable(false);
                } else if (selectedFile.endsWith(".geojson")) {

                    task = new Thread(() -> {
                        setDisable(true);
                        setText("Loading GeoJson file");
                        services.getGeoJsonService().loadGeoJsonData(selectedFile);
                        setDisable(false);
                        setText("Select data file");
                        selectedFile = null;
                        task = null;
                    });
                    task.start();
                }
            });
        });
    }

    private MenuItem createMenuItem(String text) {
        MenuItem menuItem = new MenuItem(text);
        menuItem.setOnAction(_ -> {
            selectedFile = text;
            setText("Load " + selectedFile);
        });
        return menuItem;
    }
}

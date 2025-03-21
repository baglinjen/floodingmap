package dk.itu.ui.components;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.services.Services;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

public class DataLoadComponent extends SplitMenuButton {
    private String selectedFile = null;
    private Thread task = null;
    public DataLoadComponent() {
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
            Services.withServices(services -> {
                if (selectedFile == null) return;
                if (selectedFile.endsWith(".osm")) {
                    if (task != null) {
                        return;
                    }
                    task = new Thread(() -> {
                        setDisable(true);
                        setText("Loading OSM file");
                        services.getOsmService().loadOsmDataInDb(selectedFile);
                        setDisable(false);
                        setText("Select data file");
                        selectedFile = null;
                        task = null;
                    });
                    task.start();
                    return;
                }
                if (selectedFile.endsWith(".geojson")) {
                    if (task != null) {
                        return;
                    }
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

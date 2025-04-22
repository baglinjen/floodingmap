package dk.itu.ui.components;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.data.services.Services;
import dk.itu.ui.State;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

public class DataLoadComponent extends SplitMenuButton {
    private String selectedFile = null;
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
            if (selectedFile == null || (!selectedFile.endsWith(".osm") && !selectedFile.endsWith(".gml"))) {
                return;
            }
            Services.withServices(services -> {
                if (selectedFile.endsWith(".osm")) {
                    setDisable(true);
                    setText("Loading OSM file");
                    services.getOsmService(state.isWithDb()).loadOsmData(selectedFile);
                    setDisable(false);
                    setText(selectedFile);
                } else if (selectedFile.endsWith(".gml")) {
                    setDisable(true);
                    setText("Loading GML file");
                    services.getHeightCurveService().loadGmlFileData(selectedFile);
                    setDisable(false);
                    setText(selectedFile);
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

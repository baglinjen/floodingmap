package dk.itu;

import dk.itu.data.dto.OsmParserResult;
import dk.itu.data.models.parser.ParserOsmWay;
import dk.itu.data.parsers.OsmParser;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.itu.PolygonTriangulator.triangulatePolygon;

public class App extends Application {
    @Override
    public void start(Stage stage) {

        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse("tuna.osm", osmParserResult);

        // Filter and sort data for visual purposes
        osmParserResult.sanitize();

        var osmWayPolygons = osmParserResult
                .getElementsToBeDrawn()
                .parallelStream()
                .filter(ParserOsmWay.class::isInstance)
                .map(ParserOsmWay.class::cast)
                .filter(e -> !e.isLine())
                .toList();

        var firstOsmWayPolygon = osmWayPolygons.getFirst();
        var firstOsmWayPolygonCoordinates = firstOsmWayPolygon.getCoordinates();

        float[] color = new float[3];
        firstOsmWayPolygon.getRgbaColor().getColorComponents(color);

        var data = triangulatePolygon(firstOsmWayPolygonCoordinates, color);

        var state = new State();

        var scene = new Scene(
                new StackPane(
                        new MapComponent(state, data.component1(), data.component2())
                ),
                800,
                600
        );

        scene.setOnScroll(event -> {
            if (event.getDeltaY() > 0) {
                state.zoomIn();
            } else {
                state.zoomOut();
            }
        });

        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case KeyCode.W -> {
                    // Go up
                    state.goUp();
                }
                case KeyCode.S -> {
                    // Go down
                    state.goDown();
                }
                case KeyCode.A -> {
                    // Go left
                    state.goLeft();
                }
                case KeyCode.D -> {
                    // Go right
                    state.goRight();
                }
            }
        });

        stage.setScene(scene);
        stage.setTitle("openglfx example");
        stage.show();
    }
}

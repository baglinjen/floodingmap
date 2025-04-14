package dk.itu;

import dk.itu.data.dto.OsmParserResult;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;
import dk.itu.data.parsers.OsmParser;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import kotlin.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static dk.itu.PolygonTriangulator.triangulatePolygon;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        OsmParserResult osmParserResult = new OsmParserResult();

        // Get data from OSM file
        OsmParser.parse("tuna.osm", osmParserResult);

        // Filter and sort data for visual purposes
        osmParserResult.sanitize();

        var osmElementsToBeDrawn = osmParserResult.getElementsToBeDrawn();

        double[] bounds = new double[] {Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE};

        for (var element : osmElementsToBeDrawn) {
            var elementBounds = element.getBounds();
            if (elementBounds[0] < bounds[0]) bounds[0] = elementBounds[0];
            if (elementBounds[1] < bounds[1]) bounds[1] = elementBounds[1];
            if (elementBounds[2] > bounds[2]) bounds[2] = elementBounds[2];
            if (elementBounds[3] > bounds[3]) bounds[3] = elementBounds[3];
        }

        var drawableElements = osmElementsToBeDrawn
                .parallelStream()
                .map(element ->
                    switch (element) {
                        case ParserOsmWay w -> new DrawableElement(w);
                        case ParserOsmRelation r -> new DrawableElement(r);
                        default -> null;
                    }
                )
                .filter(Objects::nonNull)
                .toList();

        var vertexData = ShapeProcessor.processDrawableElements(drawableElements);

//        var osmWayPolygons = osmParserResult
//                .getElementsToBeDrawn()
//                .parallelStream()
//                .filter(ParserOsmWay.class::isInstance)
//                .map(ParserOsmWay.class::cast)
//                .filter(e -> !e.isLine())
//                .toList();
//
//        var firstOsmWayPolygon = osmWayPolygons.getFirst();
//        var firstOsmWayPolygonCoordinates = firstOsmWayPolygon.getCoordinates();
//
//        float[] color = new float[3];
//        firstOsmWayPolygon.getRgbaColor().getColorComponents(color);
//
//        var data = triangulatePolygon(firstOsmWayPolygonCoordinates, color);

        var state = new State(bounds);

        var scene = new Scene(
                new StackPane(
                        new MapComponent(state, vertexData)
                ),
                800,
                600
        );

        stage.setScene(scene);
        stage.setTitle("Flooding map");
        stage.show();
    }
}

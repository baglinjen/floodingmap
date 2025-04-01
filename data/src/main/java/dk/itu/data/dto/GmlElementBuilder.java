package dk.itu.data.dto;

import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static dk.itu.util.CoordinateConverter.convertUTMToLatLon;

public class GmlElementBuilder {
    // Logger
    private final Logger logger = LoggerFactory.getLogger();
    // Parsed so far
    private final GmlParserResult result;
    // Fields
    private String currentId = null;
    private double[] coordinates = null;
    private Float height = null;
    // Status
    private boolean valid = true;

    public GmlElementBuilder(GmlParserResult result) {
        this.result = result;
    }

    public void buildAndAddElement() {
        if (!valid) {
            logger.warn("Invalid GML Element");
        } else {
            result.addElement(height, currentId, coordinates);
        }
        currentId = null;
        coordinates = null;
        height = null;
        valid = true;
    }

    public void withId(String currentId) {
        this.currentId = currentId;
    }
    public void withCoords(String coords) {
        var coordsList = coords.split(" ");
        if (coordsList.length < 5) {
            valid = false;
            return;
        }
//        this.currentId += "-" + coordsList[0] + "-" + coordsList[1] + "-" + coordsList[3] + "-" + coordsList[4] + "-" + coordsList[coordsList.length-3] + "-" + coordsList[coordsList.length-2];
        this.height = Float.parseFloat(coordsList[2]);
        List<Double> coordinates = new ArrayList<>((coordsList.length/3)*2);
        for (int i = 0; i < coordsList.length; i+=3) {
            var latLon = convertUTMToLatLon(Double.parseDouble(coordsList[i]), Double.parseDouble(coordsList[i+1]));
            coordinates.add(latLon[1]);
            coordinates.add(latLon[0]);
        }
        this.coordinates = coordinates.parallelStream().mapToDouble(Double::doubleValue).toArray();
    }
}
package dk.itu.data.dto;

import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static dk.itu.util.CoordinateConverter.convertUTMToLatLon;

public class HeightCurveElementBuilder {
    // Logger
    private final Logger logger = LoggerFactory.getLogger();
    // Parsed so far
    private final HeightCurveParserResult result;
    // Fields
    private Long gmlId = null;
    private double[] coordinates = null;
    private Float height = null;
    // Status
    private boolean valid = true;

    public HeightCurveElementBuilder(HeightCurveParserResult result) {
        this.result = result;
    }

    public void buildAndAddElement() {
        if (!valid || height == null || gmlId == null) {
            logger.warn("Invalid GML Element");
        } else {
            result.addParsedElement(
                    new ParserHeightCurveElement(
                            gmlId,
                            coordinates,
                            height
                    )
            );
        }
        gmlId = null;
        coordinates = null;
        height = null;
        valid = true;
    }

    public void withGmlId(long gmlId) {
        this.gmlId = gmlId;
    }
    public void withEPSG25832Coords(String coords) {
        var coordsList = coords.split(" ");
        if (coordsList.length < 5) {
            valid = false;
            return;
        }
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
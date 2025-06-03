package dk.itu.data.dto;

import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.LoggerFactory;
import org.apache.logging.log4j.Logger;

import static dk.itu.util.CoordinateUtils.utmToWgs;

public class HeightCurveElementBuilder {
    private static final Logger logger = LoggerFactory.getLogger();
    // Parsed so far
    private final HeightCurveParserResult result;
    // Fields
    private Long gmlId = null;
    private float[] coordinates = null;
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
            synchronized (result) {
                result.addParsedElementSync(
                        new ParserHeightCurveElement(
                                gmlId,
                                coordinates,
                                height
                        )
                );
            }
        }
        gmlId = null;
        coordinates = null;
        height = null;
        valid = true;
    }

    public void withGmlId(long gmlId) {
        if (!valid) return;
        this.gmlId = gmlId;
    }

    public void withHeight(float height) {
        if (!valid) return;
        this.height = height;
    }

    public void withEPSG25832Coords(String coords) {
        if (!valid) return;

        var coordsList = coords.split(" ");
        if (coordsList.length < 5) {
            valid = false;
            return;
        }

        if (containsHeightData(coordsList)){
            withEPSG25832CoordsWithHeight(coordsList);
        } else {
            withEPSG25832CoordsWithoutHeight(coordsList);
        }
    }

    //If the third-placed element is NOT heights, then a lat & lon will be compared and should not be equal (except for extreme edge cases which are not relevant for Denmark)
    private boolean containsHeightData(String[] coordsList){
        var firstPotentialHeight = coordsList[2];
        var lastPotentialHeight = coordsList[coordsList.length - 1];

        return firstPotentialHeight.equals(lastPotentialHeight);
    }

    public void withEPSG25832CoordsWithHeight(String[] coordsList) {
        this.coordinates = new float[coordsList.length / 3 * 2];
        int indexCoordinates = 0;
        for (int i = 0; i < coordsList.length; i+=3) {
            var lonLat = utmToWgs(Float.parseFloat(coordsList[i]), Float.parseFloat(coordsList[i+1]));
            this.coordinates[indexCoordinates++] = lonLat[0];
            this.coordinates[indexCoordinates++] = lonLat[1];
        }
    }

    public void withEPSG25832CoordsWithoutHeight(String[] coordsList) {
        this.coordinates = new float[coordsList.length];
        int indexCoordinates = 0;
        for (int i = 0; i < coordsList.length; i+=2) {
            var lonLat = utmToWgs(Float.parseFloat(coordsList[i]), Float.parseFloat(coordsList[i+1]));
            this.coordinates[indexCoordinates++] = lonLat[0];
            this.coordinates[indexCoordinates++] = lonLat[1];
        }
    }
}
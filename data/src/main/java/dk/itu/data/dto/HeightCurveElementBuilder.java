package dk.itu.data.dto;

import dk.itu.data.models.parser.ParserHeightCurveElement;
import dk.itu.util.LoggerFactory;
import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.DoubleStream;

import static dk.itu.util.CoordinateUtils.utmToWgs;

public class HeightCurveElementBuilder {
    // Logger
    private static final Logger logger = LoggerFactory.getLogger();
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

    public void withEPSG25832Coords(String coords) {
        if (!valid) return;

        var coordsList = coords.split(" ");
        if (coordsList.length < 5 || coordsList.length % 3 != 0) {
            valid = false;
            return;
        }

        this.height = Float.parseFloat(coordsList[2]);

        this.coordinates = ListUtils
                .partition(List.of(coordsList), 3)
                .parallelStream()
                .flatMapToDouble(set -> {
                    var lonLat = utmToWgs(Double.parseDouble(set.getFirst()), Double.parseDouble(set.get(1)));
                    return DoubleStream.of(lonLat[0], lonLat[1]);
                })
                .toArray();
    }
}
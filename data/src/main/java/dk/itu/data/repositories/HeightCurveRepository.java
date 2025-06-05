package dk.itu.data.repositories;

import dk.itu.data.models.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserHeightCurveElement;

import java.util.List;
import java.util.Map;

public interface HeightCurveRepository {
    void add(List<ParserHeightCurveElement> elements);
    void addParsedId(long id);
    boolean hasIdsBeenParsed(long id);
    Map<Float, List<ParserHeightCurveElement>> getUnconnectedElements();
    void setUnconnectedElements(Map<Float, List<ParserHeightCurveElement>> elements);
    void getElements(List<HeightCurveElement> heightCurves);
    List<List<HeightCurveElement>> getFloodingSteps(float waterLevel);
    HeightCurveElement getHeightCurveForPoint(float lon, float lat);
    float getMinWaterLevel();
    float getMaxWaterLevel();
    void clear();
}

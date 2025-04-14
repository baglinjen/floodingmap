package dk.itu.data.repositories;

import dk.itu.data.models.db.heightcurve.HeightCurveElement;
import dk.itu.data.models.parser.ParserHeightCurveElement;

import java.util.List;
import java.util.Set;

public interface HeightCurveRepository {
    void add(List<ParserHeightCurveElement> elements);
    Set<Long> getParsedIds();
    List<ParserHeightCurveElement> getUnconnectedElements();
    void setUnconnectedElements(List<ParserHeightCurveElement> elements);
    List<HeightCurveElement> getElements();
    List<List<HeightCurveElement>> getFloodingSteps(float waterLevel);
    HeightCurveElement getHeightCurveForPoint(double lon, double lat);
    float getMinWaterLevel();
    float getMaxWaterLevel();
    void clear();
}

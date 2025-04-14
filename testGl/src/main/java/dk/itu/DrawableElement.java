package dk.itu;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;

import java.util.ArrayList;
import java.util.List;

public class DrawableElement {
    private List<float[]> coordinates;
    private List<float[]> innerPolygons;
    private float[] color = new float[3];
    private boolean isLine;

    public DrawableElement(ParserOsmWay parserOsmWay) {
        this.coordinates = new ArrayList<>();
        this.coordinates.add(Floats.toArray(Doubles.asList(parserOsmWay.getCoordinates())));
        this.innerPolygons = new ArrayList<>();
        parserOsmWay.getRgbaColor().getColorComponents(this.color);
        this.isLine = parserOsmWay.isLine();
    }
    public DrawableElement(ParserOsmRelation parserOsmRelation) {
        this.coordinates = parserOsmRelation.getOuterPolygons().parallelStream().map(e -> Floats.toArray(Doubles.asList(e))).toList();
        this.innerPolygons = parserOsmRelation.getInnerPolygons().parallelStream().map(e -> Floats.toArray(Doubles.asList(e))).toList();
        parserOsmRelation.getRgbaColor().getColorComponents(this.color);
        this.isLine = false;
    }

    public List<float[]> getCoordinates() {
        return this.coordinates;
    }

    public List<float[]> getInnerPolygons() {
        return this.innerPolygons;
    }

    public float[] getColor() {
        return this.color;
    }

    public boolean isLine() {
        return this.isLine;
    }
}

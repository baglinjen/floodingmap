package dk.itu.data.models.parser;

import dk.itu.common.models.Geographical2D;
import dk.itu.common.models.OsmElement;

import java.awt.geom.Path2D;

public abstract class ParserOsmElement extends ParserDrawable implements OsmElement, Geographical2D {
    private final long id;

    public ParserOsmElement(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    public abstract double getArea();
    public abstract Path2D.Double getShape();
}


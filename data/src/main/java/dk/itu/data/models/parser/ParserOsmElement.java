package dk.itu.data.models.parser;

import dk.itu.common.models.Geographical2D;

import java.io.Serializable;

public abstract class ParserOsmElement extends ParserDrawable implements Geographical2D, Serializable {
    private final long id;

    public ParserOsmElement(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public abstract double getArea();
}
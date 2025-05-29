package dk.itu.data.models.parser;

import dk.itu.common.models.WithId;
import dk.itu.common.models.WithStyle;

public interface ParserOsmElement extends WithId, WithStyle {
    boolean shouldBeDrawn();
}
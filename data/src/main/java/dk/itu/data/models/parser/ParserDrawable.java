package dk.itu.data.models.parser;

import dk.itu.common.models.Colored;

public abstract class ParserDrawable extends Colored {
    private boolean shouldBeDrawn = true;

    public void setShouldBeDrawn(boolean shouldBeDrawn) {
        this.shouldBeDrawn = shouldBeDrawn;
    }

    public boolean shouldBeDrawn() {
        return shouldBeDrawn;
    }
}
package dk.itu.data.models.db;

import dk.itu.common.models.Colored;
import dk.itu.common.models.OsmElement;

import java.io.Serializable;

public abstract class DbOsmElement extends Colored implements OsmElement, Serializable {
    private final long id;

    public DbOsmElement(long id) {
        this.id = id;
    }

    @Override
    public long id() {
        return id;
    }
}
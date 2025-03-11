package dk.itu.data.repositories;

import dk.itu.common.models.osm.OsmElement;

import java.util.Arrays;
import java.util.List;

public class OsmElementRepository {
    @SafeVarargs
    public final void add(List<OsmElement>... osmElements) {
        Arrays.stream(osmElements).parallel().forEach(this::add);
    }
    public void add(List<OsmElement> osmElements) {
        // TODO: Add OSM Elements
        throw new UnsupportedOperationException();
    }

    public boolean connectionEstablished() {
        return false;
    }

    public boolean areElementsInDatabase() {
        return false;
    }
}

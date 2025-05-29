package dk.itu.data.models.osm;

import dk.itu.common.models.WithId;
import dk.itu.common.models.WithBoundingBoxAndArea;

import java.io.Serializable;

public interface OsmElement extends Serializable, WithId, WithBoundingBoxAndArea {}
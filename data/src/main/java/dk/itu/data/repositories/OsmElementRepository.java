package dk.itu.data.repositories;

import dk.itu.common.models.osm.OsmElement;
import dk.itu.data.dbmodels.DbNode;
import dk.itu.data.dbmodels.DbRelation;
import dk.itu.data.dbmodels.DbWay;
import dk.itu.data.dbmodels.DbGeoJson;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.geom.Geometry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OsmElementRepository {
    private static final String URL = "jdbc:postgresql://localhost:5433/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "password";
    private static final WKTReader wktReader = new WKTReader();

    private DSLContext context;

    public OsmElementRepository() {
        try {
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            this.context = DSL.using(connection, SQLDialect.POSTGRES);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @SafeVarargs
    public final void add(List<OsmElement>... osmElements) {
        Arrays.stream(osmElements).parallel().forEach(this::add);
    }
    public void add(List<OsmElement> osmElements) {
        // TODO: Add OSM Elements
        throw new UnsupportedOperationException();
    }

    public boolean connectionEstablished() {
        return this.context != null;
    }

    public boolean areElementsInDatabase() {
        return context.fetchCount(DSL.table("nodes")) > 0;
    }

    public List<OsmElement> getOsmElements() throws ParseException {
        List<OsmElement> elements = new ArrayList<>();

        // NODES
        var nodes = context.select(DSL.field("id"), DSL.field("ST_AsText(coordinate)"))
                .from("nodes")
                .fetch();

        for (var record : nodes) {
            long id = record.get("id", long.class);
            Geometry geometry = wktReader.read(record.get("st_astext", String.class));
            elements.add(new DbNode(id, (Point) geometry));
        }

        // WAYS
        var ways = context.select(DSL.field("id"),
                    DSL.field("ST_AsText(line)"),
                    DSL.field("ST_AsText(polygon)"))
                .from("ways")
                .fetch();

        for (var record : ways) {
            long id = record.get("id", long.class);
            String lineStringText = record.get("st_astext", String.class);
            Geometry geometry = (lineStringText != null) ? wktReader.read(lineStringText) : null;
            elements.add(new DbWay(id, geometry));
        }

        // RELATIONS
        var relations = context.select(DSL.field("id"), DSL.field("ST_AsText(shape)"))
                .from("relations")
                .fetch();

        for (var record : ways) {
            long id = record.get("id", long.class);
            Geometry geometry = wktReader.read(record.get("st_astext", String.class));
            elements.add(new DbRelation(id, (MultiPolygon) geometry));
        }

        // GEO JSON
        var geoJsons = context.select(DSL.field("id"),
                    DSL.field("height"),
                    DSL.field("ST_AsText(shape)"))
                .from("geoJson")
                .fetch();

        for (var record : geoJsons) {
            long id = record.get("id", long.class);
            float height = record.get("height", Float.class);
            Geometry geometry = wktReader.read(record.get("st_astext", String.class));
            elements.add(new DbGeoJson(id, (MultiPolygon) geometry, height));
        }

        return elements;
    }
}

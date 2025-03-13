package dk.itu.data.repositories;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.common.models.osm.OsmElement;
import dk.itu.common.models.osm.OsmNode;
import org.jooq.DSLContext;
import org.jooq.Geometry;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
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
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OsmElementRepository implements AutoCloseable {
    private final Connection connection;
    private final DSLContext ctx;
    private static final WKTReader wktReader = new WKTReader();

    public OsmElementRepository() throws SQLException {
        var credentials = CommonConfiguration.getInstance().getSqlCredentials();
        connection = DriverManager.getConnection(credentials.url(), credentials.username(), credentials.password());
        ctx = DSL.using(connection, SQLDialect.POSTGRES);
    }

    public final void add(List<OsmElement> osmElements) {
        add((OsmNode) osmElements.parallelStream().filter(osmElement -> osmElement instanceof OsmNode).toList().getFirst());
    }

    private void add(OsmNode osmNode) {
        ctx.insertInto(DSL.table("nodes"), DSL.field("id"), DSL.field("coordinate"))
                .values(
                        osmNode.id,
                        DSL.field("ST_GeomFromText({0}, {1})",
                                Geometry.class,
                                DSL.val(String.format("POINT(%s %s)", osmNode.getLon(), osmNode.getLat())),
                                DSL.val(4326)
                        )
                )
                .execute();
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

    public boolean areElementsInDatabase() {
        return ctx.fetchCount(DSL.table("nodes")) > 0;
    }
}

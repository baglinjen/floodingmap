package dk.itu.data.repositories;

import dk.itu.common.configurations.CommonConfiguration;
import dk.itu.common.models.OsmElement;
import dk.itu.data.models.db.DbNode;
import dk.itu.data.models.db.DbRelation;
import dk.itu.data.models.db.DbWay;
import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;
import org.apache.fury.Fury;
import org.apache.fury.ThreadLocalFury;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.config.CompatibleMode;
import org.apache.fury.config.Language;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.awt.geom.Path2D;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dk.itu.util.shape.PolygonUtils.isPolygonContained;

public class OsmElementRepository implements AutoCloseable {
    private static final ThreadSafeFury fury = new ThreadLocalFury(classLoader -> {
        Fury f = Fury.builder()
                .withLanguage(Language.JAVA)
                .withClassLoader(classLoader)
                .withRefTracking(false)
                .withCompatibleMode(CompatibleMode.SCHEMA_CONSISTENT)
                .withAsyncCompilation(true)
                .build();
        f.register(Path2D.Double.class);
        return f;
    });
    private final Connection connection;
    private final DSLContext ctx;

    public OsmElementRepository() throws SQLException {
        var credentials = CommonConfiguration.getInstance().getSqlCredentials();
        connection = DriverManager.getConnection(credentials.url(), credentials.username(), credentials.password());
        ctx = DSL.using(connection, SQLDialect.POSTGRES);
    }

    public final void add(List<ParserOsmElement> osmElements) {
        ctx.batch(
                IntStream.range(0, osmElements.size())
                        .parallel()
                        .mapToObj(i -> switch (osmElements.get(i)) {
                            case ParserOsmNode osmNode -> addNodeQuery(osmNode, i);
                            case ParserOsmWay osmWay -> addWayQuery(osmWay, i);
                            case ParserOsmRelation osmRelation -> addRelationQuery(osmRelation, i);
                            default -> null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        ).execute();
    }

    private Query addNodeQuery(ParserOsmNode osmNode, int index) {
        return ctx.insertInto(DSL.table("nodes"),
                DSL.field("id"),
                DSL.field("coordinate"),
                DSL.field("drawingOrder")
        ).values(
                osmNode.getId(),
                DSL.field("ST_GeomFromText({0}, {1})",
                        Geometry.class,
                        DSL.val(String.format("POINT(%s %s)", osmNode.getLon(), osmNode.getLat())),
                        DSL.val(4326)
                ),
                DSL.val(index)
        ).onConflict(DSL.field("id")).doNothing();
    }

    private Query addWayQuery(ParserOsmWay osmWay, int index) {
        return ctx.insertInto(DSL.table("ways"),
                DSL.field("id"),
                DSL.field("line"),
                DSL.field("polygon"),
                DSL.field("shapeSerialized"),
                DSL.field("color"),
                DSL.field("drawingOrder")
        ).values(
                osmWay.getId(),
                osmWay.isLine() ? getGeometryFieldFromShape("line", osmWay.getCoordinates()) : null,
                osmWay.isLine() ? null : getGeometryFieldFromShape("polygon", osmWay.getCoordinates()),
                fury.serializeJavaObject(osmWay.getShape()),
                DSL.val(osmWay.getRgbaColor().hashCode()),
                DSL.val(index)
        ).onConflict(DSL.field("id")).doNothing();
    }

    private Query addRelationQuery(ParserOsmRelation osmRelation, int index) {
        return ctx.insertInto(DSL.table("relations"),
                DSL.field("id"),
                DSL.field("shape"),
                DSL.field("shapeSerialized"),
                DSL.field("color"),
                DSL.field("drawingOrder")
        ).values(
                osmRelation.getId(),
                getMultipolygonGeometry(osmRelation),
                fury.serializeJavaObject(osmRelation.getShape()),
                DSL.val(osmRelation.getRgbaColor().hashCode()),
                DSL.val(index)
        ).onConflict(DSL.field("id")).doNothing();
    }

    private Field<Geometry> getMultipolygonGeometry(ParserOsmRelation osmRelation) {
        var outerPolygons = osmRelation.getOuterPolygons();
        var innerPolygons = osmRelation.getInnerPolygons();

        // Multipolygon start
        StringBuilder sb = new StringBuilder("MULTIPOLYGON(");
        for (int i = 0; i < outerPolygons.size(); i++) {
            if (i == 0) {
                sb.append("("); // First polygon start
            } else {
                sb.append(", ("); // Nth polygon start
            }
            sb.append("("); // Polygon outer start
            List<String> coordinatePairsOuter = new ArrayList<>();
            for (int l = 0; l < outerPolygons.get(i).length; l+=2) {
                coordinatePairsOuter.add(outerPolygons.get(i)[l] + " " + outerPolygons.get(i)[l+1]);
            }
            sb.append(String.join(", ", coordinatePairsOuter));
            sb.append(")"); // Polygon outer end

            // Check if there are any inner polygons
            for (double[] innerPolygon : innerPolygons) {
                if (isPolygonContained(outerPolygons.get(i), innerPolygon)) {
                    sb.append(", ("); // Inner hole start
                    List<String> coordinatePairsInner = new ArrayList<>();
                    for (int k = 0; k < innerPolygon.length; k+=2) {
                        coordinatePairsInner.add(innerPolygon[k] + " " + innerPolygon[k+1]);
                    }
                    sb.append(String.join(", ", coordinatePairsInner));
                    sb.append(")");
                }
            }
            sb.append(")"); // Polygon end
        }
        sb.append(")"); // Multipolygon end

        return DSL.field("ST_GeomFromText({0}, {1})",
                Geometry.class,
                DSL.val(sb.toString()),
                DSL.val(4326)
        );
    }

    private Field<Geometry> getGeometryFieldFromShape(String expected, double[] coordinates) {
        List<String> coordinatePairs = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i+=2) {
            coordinatePairs.add(coordinates[i] + " " + coordinates[i+1]);
        }
        return switch (expected) {
            case "polygon" -> DSL.field("ST_GeomFromText({0}, {1})",
                    Geometry.class,
                    DSL.val(String.format("POLYGON((%s))", String.join(", ", coordinatePairs))),
                    DSL.val(4326)
            );
            case "line" -> DSL.field("ST_GeomFromText({0}, {1})",
                    Geometry.class,
                    DSL.val(String.format("LINESTRING(%s)", String.join(", ", coordinatePairs))),
                    DSL.val(4326)
            );
            default -> null;
        };
    }

    public List<OsmElement> getOsmElements(int limit, double minLon, double minLat, double maxLon, double maxLat) {
        String condWaysLine = String.format("w.line && ST_MakeEnvelope(%s, %s, %s, %s, 4326)", minLon, minLat, maxLon, maxLat);
        String condWaysPoly = String.format("w.polygon && ST_MakeEnvelope(%s, %s, %s, %s, 4326)", minLon, minLat, maxLon, maxLat);
        String condRelations = String.format("r.shape && ST_MakeEnvelope(%s, %s, %s, %s, 4326)", minLon, minLat, maxLon, maxLat);
        return ctx.select(
                DSL.field("n.id", Long.class),
                DSL.field("'point' as shapeType", String.class),
                DSL.field("NULL as shapeSerialized", byte[].class),
                DSL.field("NULL as color", Integer.class),
                DSL.field("n.drawingOrder", Integer.class),
                DSL.field("'n' as type", String.class)
            )
            .from(DSL.table("nodes").as("n"))
            .unionAll(
                ctx.select(
                    DSL.field("w.id", Long.class),
                    DSL.when(DSL.condition("w.line").isNotNull(), "line").otherwise("polygon").as("shapeType"),
                    DSL.field("w.shapeSerialized", byte[].class),
                    DSL.field("w.color", Integer.class),
                    DSL.field("w.drawingOrder", Integer.class),
                    DSL.field("'w' as type", String.class)
                )
                .from(DSL.table("ways").as("w"))
                .where(DSL.condition(condWaysLine).or(condWaysPoly))
                .unionAll(
                    ctx.select(
                        DSL.field("r.id", Long.class),
                        DSL.field("'multipolygon' as shapeType", String.class),
                        DSL.field("r.shapeSerialized", byte[].class),
                        DSL.field("r.color", Integer.class),
                        DSL.field("r.drawingOrder", Integer.class),
                        DSL.field("'r' as type", String.class)
                    )
                    .from(DSL.table("relations").as("r"))
                    .where(condRelations)
                )
            )
            .orderBy(DSL.field("drawingOrder").asc())
            .limit(limit)
            .fetch(new RecordMapper<>() {
                @Nullable
                @Override
                public OsmElement map(Record6<Long, String, byte[], Integer, Integer, String> r) {
                    return switch (r.component6()) {
                        case "n" -> new DbNode(r.component1());
                        case "w" -> new DbWay(r.component1(), fury.deserializeJavaObject(r.component3(), Path2D.Double.class), r.component2(), r.component4());
                        case "r" -> new DbRelation(r.component1(), fury.deserializeJavaObject(r.component3(),Path2D.Double.class), r.component4());
                        default -> null;
                    };
                }
            });
    }

//    public List<OsmElement> getOsmElements() {
//        List<ParserOsmElement> elements = new ArrayList<>();
//
//        // NODES
//        var nodes = ctx.select(DSL.field("id"), DSL.field("ST_AsText(coordinate)"))
//                .from("nodes")
//                .fetch();
//
//        for (var record : nodes) {
//            long id = record.get("id", long.class);
//            Geometry geometry = wktReader.read(record.get("st_astext", String.class));
//            elements.add(new DbNode(id, (Point) geometry));
//        }
//
//        // WAYS
//        var ways = ctx.select(DSL.field("id"),
//                        DSL.field("ST_AsText(line)"),
//                        DSL.field("ST_AsText(polygon)"))
//                .from("ways")
//                .fetch();
//
//        for (var record : ways) {
//            long id = record.get("id", long.class);
//            String lineStringText = record.get("st_astext", String.class);
//            Geometry geometry = (lineStringText != null) ? wktReader.read(lineStringText) : null;
//            elements.add(new DbWay(id, geometry));
//        }
//
//        // RELATIONS
//        var relations = ctx.select(DSL.field("id"), DSL.field("ST_AsText(shape)"))
//                .from("relations")
//                .fetch();
//
//        for (var record : ways) {
//            long id = record.get("id", long.class);
//            Geometry geometry = wktReader.read(record.get("st_astext", String.class));
//            elements.add(new DbRelation(id, (MultiPolygon) geometry));
//        }
//
//        // GEO JSON
//        var geoJsons = ctx.select(DSL.field("id"),
//                        DSL.field("height"),
//                        DSL.field("ST_AsText(shape)"))
//                .from("geoJson")
//                .fetch();
//
//        for (var record : geoJsons) {
//            long id = record.get("id", long.class);
//            float height = record.get("height", Float.class);
//            Geometry geometry = wktReader.read(record.get("st_astext", String.class));
//            elements.add(new DbGeoJson(id, (MultiPolygon) geometry, height));
//        }
//
//        return elements;
//    }

    public boolean areElementsInDatabase() {
        if (ctx.fetchCount(DSL.table("nodes")) > 0) return true;
        if (ctx.fetchCount(DSL.table("ways")) > 0) return true;
        return ctx.fetchCount(DSL.table("relations")) > 0;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}

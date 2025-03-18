package dk.itu.data.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        );
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
                osmWay.isLine() ? getGeometryFieldFromShape("line", osmWay.getShape()) : null,
                osmWay.isLine() ? null : getGeometryFieldFromShape("polygon", osmWay.getShape()),
                fury.serializeJavaObject(osmWay.getShape()),
                DSL.val(osmWay.getRgbaColor().hashCode()),
                DSL.val(index)
        );
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
        );
    }

    private Field<Geometry> getMultipolygonGeometry(ParserOsmRelation osmRelation) {
        var outer = osmRelation.getOuterPolygons().stream().map(coordinates -> {
            List<String> coordinatePairs = new ArrayList<>();
            for (int i = 0; i < coordinates.length; i+=2) {
                coordinatePairs.add(coordinates[i] + " " + coordinates[i+1]);
            }
            return String.format("(%s)", String.join(", ", coordinatePairs));
        }).toList();
        var inner = osmRelation.getInnerPolygons().stream().map(coordinates -> {
            List<String> coordinatePairs = new ArrayList<>();
            for (int i = 0; i < coordinates.length; i+=2) {
                coordinatePairs.add(coordinates[i] + " " + coordinates[i+1]);
            }
            return String.format("(%s)", String.join(", ", coordinatePairs));
        }).toList();

        var outerString = String.format("(%s)", String.join(", ", outer));
        var innerString = String.format("(%s)", String.join(", ", inner));

        var finalString = !osmRelation.getInnerPolygons().isEmpty() ? String.join(", ", List.of(outerString, innerString)) : outerString;

        return DSL.field("ST_GeomFromText({0}, {1})",
                Geometry.class,
                DSL.val(String.format("MULTIPOLYGON(%s)", finalString)),
                DSL.val(4326)
        );
    }

    private Field<Geometry> getGeometryFieldFromShape(String expected, Shape shape) {
        List<String> coordinatePairs = new ArrayList<>();
        double[] coords = new double[2];
        for(PathIterator pi = shape.getPathIterator(null); !pi.isDone(); pi.next())
        {
            pi.currentSegment(coords);
            coordinatePairs.add(coords[0] + " " + coords[1]);
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
        String condWaysLine = String.format("line && ST_MakeEnvelope(%s, %s, %s, %s, 4326)", minLon, minLat, maxLon, maxLat);
        String condWaysPoly = String.format("polygon && ST_MakeEnvelope(%s, %s, %s, %s, 4326)", minLon, minLat, maxLon, maxLat);
        String condRelations = String.format("shape && ST_MakeEnvelope(%s, %s, %s, %s, 4326)", minLon, minLat, maxLon, maxLat);
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
//                .where(DSL.condition(condWaysLine).or(condWaysPoly))
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
//                    .where(condRelations)
                )
            )
            .orderBy(DSL.field("drawingOrder").asc())
//            .limit(limit)
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

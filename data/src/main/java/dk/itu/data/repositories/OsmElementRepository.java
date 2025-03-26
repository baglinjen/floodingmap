package dk.itu.data.repositories;

import dk.itu.common.models.OsmElement;
import dk.itu.data.models.db.DbBounds;
import dk.itu.data.models.db.DbNode;
import dk.itu.data.models.db.DbRelation;
import dk.itu.data.models.db.DbWay;
import dk.itu.data.models.parser.ParserOsmElement;
import dk.itu.data.models.parser.ParserOsmNode;
import dk.itu.data.models.parser.ParserOsmRelation;
import dk.itu.data.models.parser.ParserOsmWay;
import org.apache.commons.collections4.ListUtils;
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
import java.sql.Connection;
import java.util.*;
import java.util.List;

import static dk.itu.util.PolygonUtils.isPolygonContained;

public class OsmElementRepository {
    private static final ThreadSafeFury fury = new ThreadLocalFury(classLoader -> {
        Fury f = Fury.builder()
                .withLanguage(Language.JAVA)
                .withClassLoader(classLoader)
                .withRefTracking(false)
                .withCompatibleMode(CompatibleMode.SCHEMA_CONSISTENT)
                .withAsyncCompilation(true)
                .build();
        f.register(Path2D.Double.class);
        f.register(DbNode.class);
        f.register(DbWay.class);
        f.register(DbRelation.class);
        f.register(Color.class);
        // TODO: For Path2D use another serializer which implements org.apache.fury.serializer.Serializer
        return f;
    });

    private final DSLContext ctx;

    public OsmElementRepository(Connection connection) {
        ctx = DSL.using(connection, SQLDialect.POSTGRES);
    }

    public final void add(List<ParserOsmElement> osmElements) {
        // TODO: Fix to work with large lists (fx Denmark)
        ListUtils
                .partition(osmElements, 1000)
                .parallelStream()
                .forEach(batch -> {
                    ctx.batch(
                            batch
                                    .parallelStream()
                                    .map(osmElement -> switch (osmElement) {
                                        case ParserOsmNode osmNode -> addNodeQuery(osmNode);
                                        case ParserOsmWay osmWay -> addWayQuery(osmWay);
                                        case ParserOsmRelation osmRelation -> addRelationQuery(osmRelation);
                                        default -> null;
                                    })
                                    .filter(Objects::nonNull)
                                    .toList()
                    ).executeAsync();
                });
    }

    private Query addNodeQuery(ParserOsmNode osmNode) {
        var geoField = DSL.field("ST_GeomFromText({0}, {1})", DSL.val(String.format("POINT(%s %s)", osmNode.getLon(), osmNode.getLat())), DSL.val(4326), Geometry.class);
        return ctx.insertInto(DSL.table("nodes"),
                DSL.field("id"),
                DSL.field("coordinate"),
                DSL.field("dbObj"),
                DSL.field("area")
        ).values(
                osmNode.getId(),
                geoField,
                DSL.val(fury.serializeJavaObject(new DbNode(osmNode.getId()))),
                DSL.field("ST_Area(ST_Envelope({0}::geometry), false)", geoField)
        ).onConflict(DSL.field("id")).doNothing();
    }

    private Query addWayQuery(ParserOsmWay osmWay) {
        var geoField = osmWay.isLine() ? getGeometryFieldFromShape("line", osmWay.getCoordinates()) : getGeometryFieldFromShape("polygon", osmWay.getCoordinates());
        return ctx.insertInto(DSL.table("ways"),
                DSL.field("id"),
                DSL.field("line"),
                DSL.field("polygon"),
                DSL.field("dbObj"),
                DSL.field("area")
        ).values(
                osmWay.getId(),
                osmWay.isLine() ? geoField : null,
                osmWay.isLine() ? null : geoField,
                fury.serializeJavaObject(new DbWay(osmWay.getId(), osmWay.getShape(), osmWay.isLine() ? "line" : "polygon", osmWay.getRgbaColor().hashCode())),
                DSL.field("ST_Area(ST_Envelope({0}::geometry), false)", geoField)
        ).onConflict(DSL.field("id")).doNothing();
    }

    private Query addRelationQuery(ParserOsmRelation osmRelation) {
        var geoField = getMultipolygonGeometry(osmRelation);
        return ctx.insertInto(DSL.table("relations"),
                DSL.field("id"),
                DSL.field("shape"),
                DSL.field("dbObj"),
                DSL.field("area")
        ).values(
                osmRelation.getId(),
                geoField,
                fury.serializeJavaObject(new DbRelation(osmRelation.getId(), osmRelation.getShape(), osmRelation.getRgbaColor().hashCode())),
                DSL.field("ST_Area(ST_Envelope({0}::geometry), false)", geoField)
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
        String condWays = String.format("COALESCE(w.line, w.polygon) && ST_MakeEnvelope(%s, %s, %s, %s, 4326)", minLon, minLat, maxLon, maxLat);
        String condRelations = String.format("r.shape && ST_MakeEnvelope(%s, %s, %s, %s, 4326)", minLon, minLat, maxLon, maxLat);
        return ctx.select(
                        DSL.field("n.dbObj", byte[].class),
                        DSL.field("'n' as type", String.class),
                        DSL.field("n.area", Float.class)
                )
                .from(DSL.table("nodes").as("n"))
                .unionAll(
                        ctx.select(
                                        DSL.field("w.dbObj", byte[].class),
                                        DSL.field("'w' as type", String.class),
                                        DSL.field("w.area", Float.class)
                                )
                                .from(DSL.table("ways").as("w"))
                                .where(DSL.condition(condWays))
                                .unionAll(
                                        ctx.select(
                                                        DSL.field("r.dbObj", byte[].class),
                                                        DSL.field("'r' as type", String.class),
                                                        DSL.field("r.area", Float.class)
                                                )
                                                .from(DSL.table("relations").as("r"))
                                                .where(condRelations)
                                )
                )
                .orderBy(DSL.field("area").desc())
                .limit(limit)
                .fetch(new RecordMapper<>() {
                    @Nullable
                    @Override
                    public OsmElement map(Record3<byte[], String, Float> r) {
                        return switch (r.component2()) {
                            case "n" -> fury.deserializeJavaObject(r.component1(), DbNode.class);
                            case "w" -> fury.deserializeJavaObject(r.component1(), DbWay.class);
                            case "r" -> fury.deserializeJavaObject(r.component1(), DbRelation.class);
                            default -> null;
                        };
                    }
                });
    }

    public void clearAll() {
        ctx.batch(
                ctx.truncate("nodes"),
                ctx.truncate("ways"),
                ctx.truncate("relations")
        ).execute();
    }

    public DbBounds getBounds() {
        return ctx.select(
                DSL.field("MIN(minLon)", double.class),
                DSL.field("MIN(minLat)", double.class),
                DSL.field("MAX(maxLon)", double.class),
                DSL.field("MAX(maxLat)", double.class)
        ).from(
                ctx.select(
                                DSL.field("ST_XMin(n.coordinate)").as(DSL.field("minLon")),
                                DSL.field("ST_YMin(n.coordinate)").as(DSL.field("minLat")),
                                DSL.field("ST_XMax(n.coordinate)").as(DSL.field("maxLon")),
                                DSL.field("ST_YMax(n.coordinate)").as(DSL.field("maxLat"))
                        )
                        .from(DSL.table("nodes").as("n"))
                .unionAll(
                        ctx.select(
                                        DSL.field("ST_XMin({0})", DSL.coalesce(DSL.field("w.line"), DSL.field("w.polygon"))).as("minLon"),
                                        DSL.field("ST_YMin({0})", DSL.coalesce(DSL.field("w.line"), DSL.field("w.polygon"))).as("minLat"),
                                        DSL.field("ST_XMax({0})", DSL.coalesce(DSL.field("w.line"), DSL.field("w.polygon"))).as("maxLon"),
                                        DSL.field("ST_YMax({0})", DSL.coalesce(DSL.field("w.line"), DSL.field("w.polygon"))).as("maxLat")
                                )
                                .from(DSL.table("ways").as("w"))
                )
                .unionAll(
                        ctx.select(
                                        DSL.field("ST_XMin(r.shape)").as("minLon"),
                                        DSL.field("ST_YMin(r.shape)").as("minLat"),
                                        DSL.field("ST_XMax(r.shape)").as("maxLon"),
                                        DSL.field("ST_YMax(r.shape)").as("maxLat")
                                )
                                .from(DSL.table("relations").as("r"))
                )
        ).fetchOne(r -> {
            if (r.component1() == null || r.component2() == null || r.component3() == null || r.component4() == null) {
                return new DbBounds(0, 0, 180, 180);
            }
            return new DbBounds(r.component1(), r.component2(), r.component3(), r.component4());
        });
    }

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
}

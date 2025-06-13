package dk.itu.data.repositories;

import dk.itu.common.models.Drawable;
import dk.itu.data.datastructure.rtree.RTreeNode;
import dk.itu.data.models.osm.OsmElement;
import dk.itu.data.models.osm.OsmNode;
import dk.itu.data.models.osm.OsmRelation;
import dk.itu.data.models.osm.OsmWay;
import it.unimi.dsi.fastutil.floats.Float2ReferenceMap;
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
import java.util.stream.Collectors;

import static dk.itu.util.PolygonUtils.isPolygonContained;

public class OsmElementRepositoryDb implements OsmElementRepository {
    private static final ThreadSafeFury fury = new ThreadLocalFury(classLoader -> {
        Fury f = Fury.builder()
                .withLanguage(Language.JAVA)
                .withClassLoader(classLoader)
                .withRefTracking(false)
                .withCompatibleMode(CompatibleMode.SCHEMA_CONSISTENT)
                .withAsyncCompilation(true)
                .build();
        f.register(Path2D.Float.class);
        f.register(OsmNode.class);
        f.register(OsmWay.class);
        f.register(OsmRelation.class);
        f.register(Color.class);
        return f;
    });

    private final DSLContext ctx;

    public OsmElementRepositoryDb(Connection connection) {
        ctx = DSL.using(connection, SQLDialect.POSTGRES);
    }

    @Override
    public final void add(List<OsmElement> osmElements) {
        ctx.batch(
                osmElements
                        .parallelStream()
                        .map(e -> switch (e) {
                            case OsmNode osmNode -> addNodeNormalQuery(osmNode);
                            case OsmWay osmWay -> addWayQuery(osmWay);
                            case OsmRelation osmRelation -> addRelationQuery(osmRelation);
                            default -> null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        ).execute();
    }

    @Override
    public final void addTraversable(List<OsmNode> osmElements) {
        ctx.batch(
                osmElements
                        .parallelStream()
                        .map(this::addNodeTraversableQuery)
                        .collect(Collectors.toList())
        ).execute();
    }

    private Query addNodeTraversableQuery(OsmNode osmNode) {
        return addNodeQuery(osmNode, "nodesTraversable");
    }

    private Query addNodeNormalQuery(OsmNode osmNode) {
        return addNodeQuery(osmNode, "nodes");
    }

    private Query addNodeQuery(OsmNode osmNode, String table) {
        var geoField = DSL.field("ST_GeomFromText({0}, {1})", DSL.val(String.format("POINT(%s %s)", osmNode.getLon(), osmNode.getLat())), DSL.val(4326), Geometry.class);

        return ctx.insertInto(DSL.table(table),
                DSL.field("id"),
                DSL.field("coordinate"),
                DSL.field("dbObj"),
                DSL.field("area")
        ).values(
                osmNode.getId(),
                geoField,
                DSL.val(fury.serializeJavaObject(osmNode)),
                DSL.field("ST_Area(ST_Envelope({0}::geometry), false)", geoField)
        ).onConflict(DSL.field("id")).doNothing();
    }

    private Query addWayQuery(OsmWay osmWay) {
        var geoField = osmWay.isLine() ? getGeometryFieldFromShape("line", osmWay.getOuterCoordinates()) : getGeometryFieldFromShape("polygon", osmWay.getOuterCoordinates());

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
                fury.serializeJavaObject(osmWay),
                DSL.field("ST_Area(ST_Envelope({0}::geometry), false)", geoField)
        ).onConflict(DSL.field("id")).doNothing();
    }

    private Query addRelationQuery(OsmRelation osmRelation) {
        var geoField = getMultipolygonGeometry(osmRelation);
        return ctx.insertInto(DSL.table("relations"),
                DSL.field("id"),
                DSL.field("shape"),
                DSL.field("dbObj"),
                DSL.field("area")
        ).values(
                osmRelation.getId(),
                geoField,
                fury.serializeJavaObject(osmRelation),
                DSL.field("ST_Area(ST_Envelope({0}::geometry), false)", geoField)
        ).onConflict(DSL.field("id")).doNothing();
    }

    private Field<Geometry> getMultipolygonGeometry(OsmRelation osmRelation) {
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
            for (float[] innerPolygon : innerPolygons) {
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
                DSL.val(4326) //Coordinate format for PostGIS -> corresponds to lat/lon
        );
    }

    private Field<Geometry> getGeometryFieldFromShape(String expected, float[] coordinates) {
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

    @Override
    public List<RTreeNode> getSpatialNodes() {
        return List.of();
    }

    @Override
    public void getOsmElementsScaled(float minLon, float minLat, float maxLon, float maxLat, float minBoundingBoxArea, Float2ReferenceMap<Drawable> osmElements) {
        String condWays = String.format("COALESCE(w.line, w.polygon) && ST_MakeEnvelope(%s, %s, %s, %s, 4326) AND w.area >= %s", minLon, minLat, maxLon, maxLat, minBoundingBoxArea);
        String condRelations = String.format("r.shape && ST_MakeEnvelope(%s, %s, %s, %s, 4326) AND r.area >= %s", minLon, minLat, maxLon, maxLat, minBoundingBoxArea);
        osmElements.putAll(
                ctx.select(
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
                .fetchMap(
                        Record3::component3,
                        r -> switch (r.component2()) {
                            case "w" -> fury.deserializeJavaObject(r.component1(), OsmWay.class);
                            case "r" -> fury.deserializeJavaObject(r.component1(), OsmRelation.class);
                            default -> null;
                        }
                )
        );
    }

    @Override
    public List<OsmNode> getTraversableOsmNodes(){
        return ctx.select(
                        DSL.field("n.dbObj", byte[].class),
                        DSL.field("'n' as type", String.class),
                        DSL.field("n.area", Float.class)
                )
                .from(DSL.table("nodesTraversable").as("n"))
                .fetch(new RecordMapper<>() {
                    @Nullable
                    @Override
                    public OsmNode map(Record3<byte[], String, Float> r) {
                        if (r.component2().equals("n")) {
                            return fury.deserializeJavaObject(r.component1(), OsmNode.class);
                        } else {
                            return null;
                        }
                    }
                });
    }

    @Override
    public OsmNode getNearestTraversableOsmNode(float lon, float lat) {
        var distCond = String.format("n.coordinate <-> 'SRID=4326;POINT(%s %s)'::geometry", lon, lat);
        return ctx.select(
                DSL.field("n.dbObj", byte[].class),
                DSL.field("'n' as type", String.class),
                DSL.field("n.area", Float.class)
        )
                .from(DSL.table("nodesTraversable").as("n"))
                .orderBy(DSL.condition(distCond))
                .limit(1)
                .fetchOne(new RecordMapper<>() {
                    @Nullable
                    @Override
                    public OsmNode map(Record3<byte[], String, Float> r) {
                        if (r.component2().equals("n")) {
                            return fury.deserializeJavaObject(r.component1(), OsmNode.class);
                        } else {
                            return null;
                        }
                    }
                });
    }

    @Override
    public void clearAll() {
        ctx.batch(
                ctx.truncate("nodes"),
                ctx.truncate("ways"),
                ctx.truncate("relations")
        ).execute();
    }

    @Override
    public float[] getBounds() {
        return ctx.select(
                DSL.field("MIN(minLon)", float.class),
                DSL.field("MIN(minLat)", float.class),
                DSL.field("MAX(maxLon)", float.class),
                DSL.field("MAX(maxLat)", float.class)
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
                return new float[]{-180, -90, 180, 90};
            }
            return new float[]{r.component1(), r.component2(), r.component3(), r.component4()};
        });
    }
}
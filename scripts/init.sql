CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_raster;
CREATE EXTENSION IF NOT EXISTS postgis_sfcgal;
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch; --needed for postgis_tiger_geocoder
--optional used by postgis_tiger_geocoder, or can be used standalone
CREATE EXTENSION IF NOT EXISTS address_standardizer;
CREATE EXTENSION IF NOT EXISTS address_standardizer_data_us;
CREATE EXTENSION IF NOT EXISTS postgis_tiger_geocoder;
CREATE EXTENSION IF NOT EXISTS postgis_topology;


DROP TABLE IF EXISTS nodes;
DROP TABLE IF EXISTS ways;
DROP TABLE IF EXISTS relations;
DROP TABLE IF EXISTS geoJson;

CREATE TABLE nodes (
    id bigint primary key,
    coordinate geometry(Point) not null,
    drawingOrder int not null
);

CREATE TABLE ways (
    id bigint primary key,
    line geometry(LineString),
    polygon geometry(Polygon),
    shapeSerialized bytea not null,
    color int not null,
    drawingOrder int not null
);

CREATE TABLE relations (
    id bigint primary key,
    shape geometry(MultiPolygon) not null,
    shapeSerialized bytea not null,
    color int not null,
    drawingOrder int not null
);

CREATE TABLE geoJson (
    id serial primary key,
    height float not null,
    shape geometry(MultiPolygon) not null
);

CREATE INDEX idx_nodes_shape ON nodes USING GIST(coordinate);
CREATE INDEX idx_nodes_drawing_order ON nodes(drawingOrder);
CREATE INDEX idx_ways_line_shape ON ways USING GIST(line);
CREATE INDEX idx_ways_polygons_shape ON ways USING GIST(polygon);
CREATE INDEX idx_ways_drawing_order ON ways(drawingOrder);
CREATE INDEX idx_relations_shape ON relations USING GIST(shape);
CREATE INDEX idx_relations_drawing_order ON relations(drawingOrder);
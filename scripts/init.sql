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
    coordinate geometry(Point) not null
);

CREATE TABLE ways (
    id bigint primary key,
    line geometry(LineString),
    polygon geometry(Polygon),
    color int not null
);

CREATE TABLE relations (
    id bigint primary key,
    shape geometry(MultiPolygon) not null,
    color int not null
);

CREATE TABLE geoJson (
    id serial primary key,
    height float not null,
    shape geometry(MultiPolygon) not null
);
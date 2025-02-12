package dk.itu.models.dbmodels;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DbLineCoord {
    @JsonProperty("Altitude")
    private double altitude;

    @JsonProperty("Latitude")
    private double latitude;

    @JsonProperty("Longitude")
    private double longitude;

    // Getters and setters
    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}

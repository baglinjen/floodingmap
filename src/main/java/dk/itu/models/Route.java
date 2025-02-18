package dk.itu.models;

import java.util.List;

public class Route {
    private final List<Long> route;
    private final double distance;

    public Route(List<Long> route, double distance){
        this.route = route;
        this.distance = distance;
    }

    public List<Long> getRoute(){return route;}
    public double getDistance(){return distance;}
}

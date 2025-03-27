package dk.itu.data.utils;

import dk.itu.common.models.OsmElement;
import dk.itu.data.services.OsmService;
import dk.itu.data.services.Services;

import java.util.ArrayList;

public class DijkstraConfiguration {
    private boolean inSelectionMode = false;
    private long startNodeId, endNodeId;

    //Getters and setters
    public void toggleSelectionMode(){
        inSelectionMode = !inSelectionMode;
    }
    public boolean isInSelectionMode(){return inSelectionMode;}

    //Getters and setters for start node
    public long getStartNodeId(){return startNodeId;}
    public void setStartNodeId(String startNodeId){
        this.startNodeId = Long.parseLong(startNodeId);
    }

    //Getters and setters for end node
    public long getEndNodeId(){return endNodeId;}
    public void setEndNodeId(String endNodeId){
        this.endNodeId = Long.parseLong(endNodeId);
    }

    public void calculateRoute(){
        Services.withServices(s -> {
           var x = s.getOsmService().getOsmNodes();

            //Ensure nodes exists
            if(x.stream().noneMatch(o -> o.getId() == startNodeId)) throw new IllegalArgumentException("Start node ID could not be found in nodes");
            if(x.stream().noneMatch(o -> o.getId() == endNodeId)) throw new IllegalArgumentException("End node ID could not be found in nodes");

            //TODO: Calculate route based on routings (have NOT yet been added to DB)
        });

    }
}

package dk.itu.data.datastructure.curvetree;

import dk.itu.data.models.parser.ParserGeoJsonElement;

import java.util.*;

public class CurveTree {
    private final List<CurveTreeNode> rootChildren = new ArrayList<>();

    public CurveTree(){
        System.out.println("Now building...");
    }

    public void put(ParserGeoJsonElement geoJsonElement) {
        put(null, geoJsonElement);
    }

    private void put(CurveTreeNode curveTreeNode, ParserGeoJsonElement geoJsonElement) {
        if (curveTreeNode == null) {
            // Adding to root
            lookupChildren(rootChildren, geoJsonElement);
        } else {
            if (curveTreeNode.getChildren() == null) {
                curveTreeNode.addChild(new CurveTreeNode(geoJsonElement));
            } else {
                lookupChildren(curveTreeNode.getChildren(), geoJsonElement);
            }
        }
    }

    private void lookupChildren(List<CurveTreeNode> children, ParserGeoJsonElement geoJsonElement) {
        for (CurveTreeNode child : children) {
            // If geoJsonElement in child => add + return
            if (child.getGeoJsonElement().contains(geoJsonElement)) {
                // Add to node + return
                put(child, geoJsonElement);
                return;
            }
            // Else => iterate
        }
        children.add(new CurveTreeNode(geoJsonElement));
    }

    ///Returns the stages at which affected height curves will be flooded
    public List<List<ParserGeoJsonElement>> TraverseFromRoot(float waterHeight){
        var result = new ArrayList<List<ParserGeoJsonElement>>();

        var map = new HashMap<Integer, List<ParserGeoJsonElement>>();
        aux(map, rootChildren, waterHeight, 0);

        //Convert map to list and return
        for(int i = 0; i < map.size(); i++){
            var tempList = new ArrayList<ParserGeoJsonElement>(map.get(i));
            if(!tempList.isEmpty()) result.add(tempList);
        }

        return result;
    }

    private void aux(HashMap<Integer, List<ParserGeoJsonElement>> result, List<CurveTreeNode> children, float waterHeight, int stepDepth){
        //Determine if the children are reachable at the current water level
        var iterationResults = new ArrayList<ParserGeoJsonElement>();
        for(var node : children){
            if(node.getGeoJsonElement().getHeight() <= waterHeight){
                //The water can reach this node
                iterationResults.add(node.getGeoJsonElement());
                if(node.getChildren() != null) aux(result, node.getChildren(), waterHeight, stepDepth + 1);
            }
        }

        var current = result.getOrDefault(stepDepth, new ArrayList<>());
        current.addAll(iterationResults);
        result.put(stepDepth, current);
    }
}

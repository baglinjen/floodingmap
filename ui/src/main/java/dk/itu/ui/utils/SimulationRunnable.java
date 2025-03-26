package dk.itu.ui.utils;

import dk.itu.data.datastructure.curvetree.CurveTree;
import dk.itu.data.models.parser.ParserGeoJsonElement;

import java.util.List;

public class SimulationRunnable {
    public static Runnable getRunnable(CurveTree curveTree, float waterHeight){
        return () -> {
            //Calculate the water spread
            var x = curveTree.TraverseFromRoot(waterHeight);

            for(List<ParserGeoJsonElement> list : x){
                list.forEach(hc -> hc.setBelowWater(waterHeight > hc.getHeight()));
                try{
                    Thread.sleep(3000);
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        };
    }
}

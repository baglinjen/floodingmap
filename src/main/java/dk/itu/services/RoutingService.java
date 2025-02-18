package dk.itu.services;

import dk.itu.models.OsmNode;
import dk.itu.models.Route;
import dk.itu.models.dbmodels.DbRouting;
import dk.itu.utils.HibernateUtil;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import java.util.*;

/// Service for constructing the shortest route between two OSMNodes
@Service
public class RoutingService {
    private static Session session;

    public RoutingService(){
        session = HibernateUtil.getSessionFactory().openSession();
    }

    public Route createRoute(long startNode, long endNode, double waterLevel){
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparing(a -> a[1]));
        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        Set<Long> visited = new HashSet<>();

        pq.offer(new long[]{startNode, 0});
        distances.put(startNode, 0.0);

        while(!pq.isEmpty()){
            long[] current = pq.poll();
            long n = current[0];
            double currentDist = current[1];

            if(!visited.add(n)) continue;

            if(n == endNode) break;

            for(DbRouting edge : TraversableFromNode(n)){
                long neighbor = edge.getToId();
                double newDist = currentDist + edge.getDistanceMeters();

                //Determine if routing is unusable due to water levels
                if(edge.getLowestAltitude() < waterLevel) continue;

                if(newDist < distances.getOrDefault(neighbor, Double.MAX_VALUE)){
                    distances.put(neighbor, newDist);
                    pq.offer(new long[]{neighbor, (long)newDist});
                    previous.put(neighbor, n);
                }
            }
        }

        return new Route(reversePath(startNode, endNode, previous), distances.getOrDefault(endNode, Double.NaN));
    }

    public Route createRoute(OsmNode startNode, OsmNode endNode, double waterLevel){
        return createRoute(startNode.getId(), endNode.getId(), waterLevel);
    }

    private List<Long> reversePath(long startNode, long endNode, Map<Long, Long> connections){
        List<Long> results = new ArrayList<>();
        for(Long at = endNode; at != null; at = connections.get(at)){
            results.add(at);
            if(at == startNode) break;
        }
        Collections.reverse(results);

        //Possibly throw exception if no route could be configured
        if(results.getFirst() != startNode){
            return Collections.emptyList();
        }

        return results;
    }

    private List<DbRouting> TraversableFromNode(Long nodeId){
        return session.createQuery("FROM DbRouting WHERE fromid = :id ORDER BY distancemeters ASC", DbRouting.class).setParameter("id", nodeId).getResultList();
    }
}

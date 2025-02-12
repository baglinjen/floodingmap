package dk.itu.services.modelservices;

import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import dk.itu.utils.HibernateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class WayService {
    private static Session session;

    public static OsmWay LoadWayFromDb(Long wayId) throws Exception {
        OsmWay way = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            way = session.get(OsmWay.class, wayId);
            if (way == null) throw new Exception("The way could not be extracted from the database");

//            //Retrive all nodes related to way
//            var query = "SELECT n FROM OsmNode n WHERE n.id IN :ids ORDER BY CASE n.id ";
            List<Long> nodeIds = way.getNodeIds();
//
//            for (int i = 0; i < nodeIds.size(); i++) {
//                query += "WHEN " + nodeIds.get(i) + " THEN " + i + " ";
//            }
//            query += "END";

//            List<OsmNode> nodes = session.createQuery(query, OsmNode.class)
//                    .setParameter("ids", nodeIds)
//                    .getResultList();

            List<OsmNode> nodes = session.createNativeQuery(
                    "SELECT * FROM nodes n WHERE n.id = ANY(:ids) ORDER BY array_position(:ids, n.id)", OsmNode.class)
                    .setParameter("ids", nodeIds.toArray(new Long[0]))
                    .getResultList();

            //Ensure way "close" if needed
            if (Objects.equals(way.getNodeIds().getFirst(), way.getNodeIds().getLast())) nodes.add(nodes.getFirst());

            way.setNodes(nodes);
            way.GeneratePath();
            return way;
        } catch (Exception e){
            System.out.println(e);
            return way;
        } finally {
            session.close();
        }
    }

    public List<OsmWay> GetAllWays(){
        session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        List<OsmWay> ways = new ArrayList<>();
        List x = session.createQuery("SELECT id FROM OsmWay").getResultList();
        for(Object l : x){
            try{
                ways.add(LoadWayFromDb((Long)l));
            } catch(Exception e){
                System.out.println("A way was not loaded");
            }
        }

        return ways;
    }
}

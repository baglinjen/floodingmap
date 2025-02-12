package dk.itu.services.modelservices;

import dk.itu.models.OsmNode;
import dk.itu.models.OsmWay;
import dk.itu.utils.HibernateUtil;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
public class WayService {
    private static Session session;

    public static List<OsmWay> LoadWaysFromDb() throws Exception {
        List<OsmWay> ways = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            session.beginTransaction();
            ways = session.createQuery("FROM OsmWay", OsmWay.class).getResultList();
            if (ways == null) throw new Exception("The ways could not be extracted from the database");

            // Retrieve all nodes related to way
            for(OsmWay way : ways)
            {
                List<Long> nodeIds = way.getNodeIds();
                List<OsmNode> nodes = session.createNativeQuery(
                                "SELECT * FROM nodes n WHERE n.id = ANY(:ids) ORDER BY array_position(:ids, n.id)", OsmNode.class)
                        .setParameter("ids", nodeIds.toArray(new Long[0]))
                        .getResultList();

                // Ensure way "close" if needed
                if (Objects.equals(nodeIds.getFirst(), nodeIds.getLast())) nodes.add(nodes.getFirst());
                way.setNodes(nodes);
                way.GeneratePath();
            }
          
            return ways;
        } catch (Exception e){
            System.out.println(e);
            return ways;
        } finally {
            session.close();
        }
    }
}

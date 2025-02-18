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

    public static List<OsmWay> LoadWaysFromDb() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            List<OsmWay> ways = session.createQuery("FROM OsmWay", OsmWay.class).getResultList();

            // Load and set nodes for each way
            for (OsmWay way : ways) {
                List<Long> nodeIds = way.getNodeIds();
                List<OsmNode> nodes = session.createNativeQuery(
                                "SELECT * FROM nodes n WHERE n.id = ANY(:ids) ORDER BY array_position(:ids, n.id)",
                                OsmNode.class)
                        .setParameter("ids", nodeIds.toArray(new Long[0]))
                        .getResultList();

                if (Objects.equals(nodeIds.getFirst(), nodeIds.getLast())) nodes.add(nodes.getFirst());
                way.setNodes(nodes);
            }

            session.close();
            return ways;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ways from database", e);
        }
    }
}

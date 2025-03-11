package dk.itu.data.dbmodels;

import dk.itu.common.models.osm.OsmElement;
import jakarta.persistence.*;

import java.awt.*;
import java.util.List;

@Entity
@Table(name = "osm_relations")
public class OsmRelation extends OsmElement {

    @ElementCollection
    @CollectionTable(name = "osm_relation_members", joinColumns = @JoinColumn(name = "relation_id"))
    private List<RelationMember> members;

    public OsmRelation(Long id, List<RelationMember> members) {
        super(id);
        this.members = members;
    }

    public List<RelationMember> getMembers() {
        return members;
    }

    public void setMembers(List<RelationMember> members) {
        this.members = members;
    }

    @Override
    public double getArea() {
        return 0;
    }

    @Override
    public Shape getShape() {
        return null;
    }

    @Override
    public void draw(Graphics2D g2d, float strokeBaseWidth) {

    }

    @Override
    public double[] getBounds() {
        return new double[0];
    }

    @Embeddable
    public static class RelationMember {
        @Column(name = "member_id")
        private Long memberId;

        @Column(name = "member_type")
        private String memberType;

        public RelationMember() {}

        public RelationMember(Long memberId, String memberType) {
            this.memberId = memberId;
            this.memberType = memberType;
        }

        public Long getMemberId() {
            return memberId;
        }

        public String getMemberType() {
            return memberType;
        }
    }
}

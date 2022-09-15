package phase2.database;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class agentlist {
    @Id
    Integer agentid;

    Integer status;

    public agentlist(){}

    public agentlist(Integer agentId, Integer status){
        this.agentid = agentId;
        this.status = status;
    }
}

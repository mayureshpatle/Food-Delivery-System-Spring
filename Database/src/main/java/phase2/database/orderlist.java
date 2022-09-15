package phase2.database;

import javax.persistence.*;

@Entity
public class orderlist {
    @Id
    Integer orderid;

    String status;

    Integer agentid;

    public  orderlist (){}

    public orderlist(Integer orderid, String status, Integer agentid){

        this.orderid = orderid;
        this.agentid = agentid;
        this.status = status;
    }

    public orderlist(Integer orderId){
        this.orderid = orderId;
        this.agentid = -1;
        this.status = "unassigned";
    }
}

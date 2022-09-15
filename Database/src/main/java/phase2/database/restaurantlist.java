package phase2.database;

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class restaurantlist {

    @EmbeddedId
    key id;

    Integer price;

    public restaurantlist(){}
    public restaurantlist(Integer restId, Integer itemId, Integer price){
        this.id = new key(restId, itemId);
        this.price = price;
    }
}

@Embeddable
class key implements Serializable {
    Integer restid;
    Integer itemid;

    public key(){}
    public key(Integer restid, Integer itemid){
        this.restid = restid;
        this.itemid = itemid;
    }
}

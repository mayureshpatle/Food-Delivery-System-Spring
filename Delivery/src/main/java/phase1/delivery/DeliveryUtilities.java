package phase1.delivery;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

// class to read the details from POST request for /requestOrder endpoint
class OrderRequest {
    public long custId, restId, itemId, qty;
    OrderRequest() {}
}

// class to request item price from database service
class Item {
    public long restId, itemId, price;
    public Item() {}
}

class AgentResponse {
    public long agentId;
    public int status;
    public AgentResponse() {}
}

// contains functions for various delivery service operations
public class DeliveryUtilities {

    String databaseService = "http://vivek-database:8080";

    // constructor
    public DeliveryUtilities() {}                               // 0

    // check if order is valid, return true if qty>0
    public boolean validOrder(OrderRequest order) {             // 1.1
        return order.qty > 0;
    }

    // returns total bill amount
    public long billAmount(OrderRequest req) {                  // 1.1
        try {
            String dbURI = databaseService + "/price/" + req.restId + "/" + req.itemId;
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Item> dbResponse = restTemplate.getForEntity(dbURI, Item.class);
            Item item = dbResponse.getBody();
            return item.price * req.qty;
        }
        catch (HttpClientErrorException e) {
            return -1;
        }
    }

    // creates new order and returns the orderId
    public long placeOrder() {                                  // 1.2
        String dbURI = databaseService + "/createOrderEntry";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity <OrderStatus> dbResponse = restTemplate.postForEntity(dbURI, "", OrderStatus.class);
        OrderStatus order = dbResponse.getBody();
        return order.orderId;                                   // return orderId for the created order
    }

    public void agentSignIn(long id) {                          // 2
        String dbURI = databaseService + "/recordAgentSignIn";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(dbURI, new AgentIdData(id), String.class);
    }

    public void agentSignOut(long id) {                         // 3
        String dbURI = databaseService + "/recordAgentSignOut";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(dbURI, new AgentIdData(id), String.class);
    }

    public void orderDelivered(long orderId) {                  // 4
        String dbURI = databaseService + "/recordOrderDelivery";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(dbURI, new OrderIdData(orderId), String.class);
    }

    // returns the order details for orderId
    public OrderStatus orderDetails(long orderId){              // 5
        try {
            String dbURI = databaseService + "/getOrderStatus/" + orderId ;
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity <OrderStatus> dbResponse = restTemplate.getForEntity(dbURI, OrderStatus.class);
            return dbResponse.getBody();
        }
        catch (HttpClientErrorException e) {
            return null;
        }
    }

    // returns the current status of agent
    public String getAgentStatus(long agentId) {                // 6
        String dbURI = databaseService + "/getAgentStatus/" + agentId;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity <AgentResponse> dbResponse = restTemplate.getForEntity(dbURI, AgentResponse.class);
        int status = dbResponse.getBody().status;
        if(status == 0) return "signed-out";
        if(status == 1) return "available";
        return "unavailable";
    }

    public void reInitialize() {                                // 7
        String dbURI = databaseService + "/reInitialize";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(dbURI, "", String.class);
    }

}
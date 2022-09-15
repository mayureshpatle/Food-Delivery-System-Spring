package phase1.delivery;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

// class to read the details from post request for /agentSignIn and /agentSignOut endpoints
class AgentIdData {
    public Long agentId;
    AgentIdData() {}
    AgentIdData(long id) {
        this.agentId = id;
    }
}

// class to send agent status as response for /agent/{agentId} endpoint
class AgentStatus {
    public Long agentId;
    public String status;
    AgentStatus() {}
    AgentStatus(long id, String status) {
        this.agentId = id;
        this.status = status;
    }
}

// class to send orderId as response for /requestOrder endpoint
class OrderIdData {
    public Long orderId;
    OrderIdData() {}
    OrderIdData(Long id) {
        this.orderId = id;
    }
}

// class to send order status as response for /order/{orderId} endpoint
class OrderStatus {
    public Long orderId;
    public String status;
    public Long agentId;
    OrderStatus() {}
}

// class to send json data for POST requests to wallet service
class WalletRequest {
    public long custId, amount;
    WalletRequest(long custId, long amount) {
        this.custId = custId;
        this.amount = amount;
    }
}

// class to send json data for POST requests to restaurant service
class RestaurantRequest {
    public long restId, itemId, qty;
    RestaurantRequest(long restId, long itemId, long qty) {
        this.restId = restId;
        this.itemId = itemId;
        this.qty = qty;
    }
}

@RestController
public class RequestController {

    String restaurantService = "http://vivek-restaurant:8080";
    String walletService = "http://vivek-wallet:8080";

    DeliveryUtilities deliveryUtils = new DeliveryUtilities();

    // 1
    @PostMapping("/requestOrder")
    public ResponseEntity <OrderIdData> requestOrder(@RequestBody OrderRequest order) {
        // validate order
        if(!deliveryUtils.validOrder(order)) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        // calculate total billing amount for order
        long billingAmount = deliveryUtils.billAmount(order);
        if(billingAmount == -1) {
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        RestTemplate restTemplate = new RestTemplate();

        //deduct billing amount from customer's wallet
        boolean deducted = false;
        String walletURI = walletService + "/deductBalance";
        WalletRequest walletRequest = new WalletRequest(order.custId, billingAmount);
        try {
            ResponseEntity<String> walletResponse = restTemplate.postForEntity(walletURI, walletRequest, String.class);
            if (walletResponse.getStatusCodeValue() == 201) {
                deducted = true;
            }
        }
        catch(HttpClientErrorException e) {
            if(e.getRawStatusCode() != 410) {
                System.out.println("Something went wrong while deducting balance!");
            }
        }

        // if billing amount is successfully deducted from customer's wallet
        if(deducted) {
            // invoke Restaurant Service's acceptOrder endpoint
            boolean orderAccepted = false;
            String restaurantURI = restaurantService + "/acceptOrder";
            RestaurantRequest restaurantRequest = new RestaurantRequest(order.restId, order.itemId, order.qty);
            try {
                ResponseEntity<String> restaurantResponse = restTemplate.postForEntity(restaurantURI, restaurantRequest, String.class);
                if (restaurantResponse.getStatusCodeValue() == 201) {
                    orderAccepted = true;
                }
            }
            catch(HttpClientErrorException e) {
                if(e.getRawStatusCode() != 410) {
                    System.out.println("Something went wrong while invoking Restaurant service!");
                }
            }

            // if restaurant service return HTTP status 201
            if(orderAccepted) {
                System.out.println(3);
                // create new order
                OrderIdData response = new OrderIdData(deliveryUtils.placeOrder());
                return ResponseEntity.status(201).body(response);
            }
            else {  //add amount back to customer's wallet
                walletURI = walletService + "/addBalance";
                try {
                    ResponseEntity<String> walletResponse = restTemplate.postForEntity(walletURI, walletRequest, String.class);
                    if (walletResponse.getStatusCodeValue() != 201) {
                        System.out.println("WRONG HTTP STATUS: Expected 201, received " + walletResponse.getStatusCodeValue() +"!");
                    }
                }
                catch(HttpClientErrorException e) {
                    System.out.println(5);
                    System.out.println("Something went wrong while adding balance!");
                }
            }
        }
        return new ResponseEntity<>(HttpStatus.GONE);
    }

    // 2
    @PostMapping("/agentSignIn")
    public ResponseEntity <String> agentSignIn(@RequestBody AgentIdData agent) {
        deliveryUtils.agentSignIn(agent.agentId);
        return ResponseEntity.status(201).body("");
    }

    // 3
    @PostMapping("/agentSignOut")
    public ResponseEntity <String> agentSignOut(@RequestBody AgentIdData agent) {
        deliveryUtils.agentSignOut(agent.agentId);
        return ResponseEntity.status(201).body("");
    }

    // 4
    @PostMapping("/orderDelivered")
    public ResponseEntity <String> orderDelivered(@RequestBody OrderIdData order) {
        deliveryUtils.orderDelivered(order.orderId);
        return ResponseEntity.status(201).body("");
    }

    // 5
    @GetMapping("/order/{orderId}")
    public ResponseEntity <OrderStatus> getOrderStatus(@PathVariable Long orderId) {
        OrderStatus response = deliveryUtils.orderDetails(orderId);
        if(response != null) {
            return ResponseEntity.status(200).body(response);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // 6
    @GetMapping("/agent/{agentId}")
    public ResponseEntity <AgentStatus> getAgentStatus(@PathVariable Long agentId) {
        AgentStatus response = new AgentStatus(agentId, deliveryUtils.getAgentStatus(agentId));
        return ResponseEntity.status(200).body(response);
    }

    // 7
    @PostMapping("/reInitialize")
    public ResponseEntity <String> reInitialize() {
        deliveryUtils.reInitialize();
        return ResponseEntity.status(201).body("");
    }

//    // verification
//    @GetMapping("/showDelivery")
//    public ResponseEntity <String> showDelivery() {
//        return ResponseEntity.status(200).body(DeliveryDetails.print());
//    }

}

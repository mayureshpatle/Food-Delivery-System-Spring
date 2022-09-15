
// NOTE:
// The function names are descriptive, unnecessary comments will degrade the readability of code

package phase2.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

class orderData{
    public Integer orderId;
    public String status;
    public Integer agentId;
    public orderData(){}
    public orderData(orderlist o){
        this.orderId = o.orderid;
        this.status = o.status;
        this.agentId = o.agentid;
    }
}

class restaurantData{
    public Integer restId;
    public Integer itemId;
    public Integer price;
    public restaurantData(){}
    public restaurantData(restaurantlist r){
        this.restId = r.id.restid;
        this.itemId = r.id.itemid;
        this.price = r.price;
    }
}

class agentData {
    public Integer agentId;
    public Integer status;
    public agentData(){}
    public agentData(agentlist a){
        this.agentId = a.agentid;
        this.status = a.status;
    }
}

class agentQuery {
    public Integer agentId;
    public agentQuery(){}
}

class orderQuery {
    public Integer orderId;
    public orderQuery(){}
}

@RestController
public class RequestController {
    private final orderlistRepository orders;
    private final agentlistRepository agents;
    private final restaurantlistRepository rests;

    static AtomicInteger lock = new AtomicInteger(0); // using this to avoid data races & inconsistencies

    // constructor
    @Autowired
    public RequestController(orderlistRepository o, agentlistRepository a, restaurantlistRepository r){
        orders = o;
        agents = a;
        rests = r;
        this.initialize();
    }


    // ------------ Agent Related Endpoints ------------ //

    // no need of serialization, directly return agent status at the moment
    @GetMapping("/getAgentStatus/{agentId}")
    public ResponseEntity<agentData> getAgentStatus(@PathVariable Integer agentId) {
        agentlist agent = agents.findById(agentId);
        return ResponseEntity.status(200).body(new agentData(agent));
    }

    // records tha agent has signed out (first ensures that agent is available)
    @PostMapping("/recordAgentSignOut")
    public ResponseEntity<String> recordAgentSignOut(@RequestBody agentQuery a) {
        acquire(lock);
        agentlist agent = agents.findById(a.agentId);
        if(agent.status == 1) {
            agent.status = 0;
            agents.save(agent);
        }
        release(lock);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // records that agent has signed in & tries to assign order if signIn is valid
    @PostMapping("/recordAgentSignIn")
    public ResponseEntity<String> recordAgentSignIn(@RequestBody agentQuery a) {
        acquire(lock);
        agentlist agent = agents.findById(a.agentId);
        if (agent.status == 0) {
            assignOrderToAgent(agent.agentid);
        }
        release(lock);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // utility function to assign order to agent
    // used in /recordAgentSignIn and /recordOrderDelivery endpoints
    void assignOrderToAgent(int agentId) {
        agentlist agent = agents.findById(agentId);
        try {
            int orderId = orders.getUnassignedOrder();
            agent.status = 2;
            orders.save(new orderlist(orderId, "assigned", agent.agentid));
            agents.save(agent);
        }
        catch (NullPointerException e){
            agent.status = 1;
            agents.save(agent);
        }
        System.out.println(agent.status);
    }



    // ------------ Restaurant (Item Cost) Related Endpoints ------------ //

    // return the cost of item (itemId) from given restaurant (restId)
    // no need of serialization
    @GetMapping("/price/{restId}/{itemId}")
    public ResponseEntity<restaurantData> price(@PathVariable Integer restId, @PathVariable Integer itemId){
        restaurantlist item = rests.findById(new key(restId, itemId));
        if(item == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(200).body(new restaurantData(item));
    }



    // ------------ Order Related Endpoints ------------ //

    // no need of serialization, directly return order status at the moment
    @GetMapping("/getOrderStatus/{orderId}")
    public ResponseEntity<orderData> getOrderStatus(@PathVariable Integer orderId){
        // hardcoding for this value because we are using orderId 999 as base for sequence starting
        if(orderId == 999) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        orderlist order = orders.findById(orderId);
        if(order == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(200).body(new orderData(order));
    }

    // creates a new row for order entry, with implicit autoincrement feature on orderId
    // also tries to assign available agent with minimum agentId
    @PostMapping("/createOrderEntry")
    public ResponseEntity<orderData> createOrderEntry(){
        acquire(lock);
        int orderId = orders.getMaxOrderId() + 1;
        int agentId = -1;
        String status = "unassigned";

        // try to assign agent
        try  {
            agentId = agents.getAvailableAgent();
            agents.save(new agentlist(agentId, 2));
            status = "assigned";
        }
        catch (NullPointerException e) {}

        orderlist order = new orderlist(orderId, status, agentId);
        orders.save(order);
        release(lock);
        return ResponseEntity.status(201).body(new orderData(order));
    }

    // records that the order was delivered and tries to assign order to assigned agent
    @PostMapping("/recordOrderDelivery")
    public ResponseEntity<String> recordOrderDelivery(@RequestBody orderQuery o){
        acquire(lock);
        orderlist order = orders.findById(o.orderId);

        if(order != null && order.status.equals("assigned")) {
            order.status = "delivered";
            orders.save(order);
            assignOrderToAgent(order.agentid);
        }
        release(lock);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }



    // ------------ ReInitialize Endpoint ------------ //

    // reinitialize endpoint
    @PostMapping("/reInitialize")
    public ResponseEntity<String> reInitialize(){
        acquire(lock);
        orders.truncate();
        this.initialize();
        release(lock);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/dropAll")
    public String dropAll(){
        orders.dropTable();
        rests.dropTable();
        agents.dropTable();
        return "";
    }

    // reads initialData.txt file and load the data in database
    void initialize() {
        try {
            String inputFile = "initialData.txt";
            File initialData = new File(inputFile);
            Scanner myReader = new Scanner(initialData);
            int count = 3;
            while (count>1 && myReader.hasNextLine()) {
                String currentLine = myReader.nextLine().trim();
                if(currentLine.equals("****")) {
                    --count;
                }
                // restaurant section
                else if (count == 3) {
                    String [] line = currentLine.split(" ");

                    Integer restId = Integer.parseInt(line[0]);
                    int itemCount = Integer.parseInt(line[1]);

                    // item details section
                    while(itemCount-- > 0) {
                        currentLine = myReader.nextLine().trim();
                        line = currentLine.split(" ");

                        Integer itemId = Integer.parseInt(line[0]);
                        Integer price = Integer.parseInt(line[1]);
                        restaurantlist r = new restaurantlist(restId, itemId, price);
                        rests.save(r);
                    }
                }
                // agentId section
                else if (count == 2) {
                    Integer agentId = Integer.parseInt(currentLine);
                    agentlist a = new agentlist(agentId, 0);
                    agents.save(a);
                }
            }
            myReader.close();
            orders.save(new orderlist(999, "delivered", -1));
        } catch (FileNotFoundException e) {
            System.out.println("Initializer File Not Found.");
            e.printStackTrace();
        }
    }


    // ------------ Functions for Locking Operations ------------ //

    void acquire(AtomicInteger x){
        while(!x.compareAndSet(0,1));
    }

    void release(AtomicInteger x){
        x.set(0);
    }
}

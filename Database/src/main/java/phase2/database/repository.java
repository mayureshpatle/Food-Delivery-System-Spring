package phase2.database;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

interface orderlistRepository extends Repository<orderlist, Integer> {

    // adds order entry to table
    void save(orderlist s);

    // returns order details
    orderlist findById(Integer orderId);
    @Modifying
    @Transactional(rollbackFor = Exception.class)

    // empties orderlist
    @Query(value = "TRUNCATE TABLE orderlist", nativeQuery = true)
    void truncate();

    // returns maximum orderid
    @Query(value = "SELECT MAX(orderid) FROM orderlist", nativeQuery = true)
    Integer getMaxOrderId();

    // return count of unassigned orders
    @Query(value = "SELECT COUNT(orderid) FROM orderlist WHERE status = 'unassigned'", nativeQuery = true)
    Integer countUnassignedOrders();

    // returns unassigned order with minimum orderId
    @Query(value = "SELECT MIN(orderid) FROM orderlist WHERE status = 'unassigned'", nativeQuery = true)
    Integer getUnassignedOrder();

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query(value = "DROP Table orderlist", nativeQuery = true)
    void dropTable();
}

interface agentlistRepository extends Repository<agentlist, Integer> {

    // adds agent entry to table
    void save(agentlist s);

    // returns agent details
    agentlist findById(Integer agentId);

    // return count of available agents
    @Query(value = "SELECT COUNT(agentid) FROM agentlist WHERE status = 1", nativeQuery = true)
    Integer countAvailableAgents();

    // return available agent with minimum agentid
    @Query(value = "SELECT MIN(agentid) FROM agentlist WHERE status = 1", nativeQuery = true)
    Integer getAvailableAgent();

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query(value = "DROP Table agentlist", nativeQuery = true)
    void dropTable();
}

interface restaurantlistRepository extends Repository<restaurantlist, key> {

    // adds (restaurant, item, cost) entry to table
    void save(restaurantlist s);

    // return details of (restaurant, item)
    restaurantlist findById(key k);

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query(value = "DROP Table restaurantlist", nativeQuery = true)
    void dropTable();
}


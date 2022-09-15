from http import HTTPStatus
from threading import Thread
import requests

# 3 orders are unassigned, and 2 concurent /agentSignIn requests come (for agents 201 & 202)
# Only 1000 and 1001 orderId's (minimum orderId's) should be assigned agents
# both agents should become unavailable

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082



# ---------------- /requestOrder requests ---------------- #

def t1(result):  # First concurrent  order request
    # Customer 301 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 1, "qty": 3})

    result["1"] = http_response


def t2(result):  # Second concurrent order request
    # Customer 302 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 302, "restId": 101, "itemId": 1, "qty": 3})

    result["2"] = http_response


def t3(result):  # Third concurrent order request
    # Customer 303 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 303, "restId": 101, "itemId": 1, "qty": 3})

    result["3"] = http_response


# ---------------- /agentSignIn requests ---------------- #

def t4(result):  # First concurrent signIn request
    # Agent 201 signs in
    http_response = requests.post(
        "http://localhost:8081/agentSignIn", json={"agentId": 201})

    result["1"] = http_response


def t5(result):  # Second concurrent signin request
    # Agent 202 signs in
    http_response = requests.post(
        "http://localhost:8081/agentSignIn", json={"agentId": 202})

    result["2"] = http_response


def test():

    result = {}

    # Reinitialize Restaurant service
    http_response = requests.post("http://localhost:8080/reInitialize")

    # Reinitialize Delivery service
    http_response = requests.post("http://localhost:8081/reInitialize")

    # Reinitialize Wallet service
    http_response = requests.post("http://localhost:8082/reInitialize")

    ### Parallel Execution Begins ###
    thread1 = Thread(target=t1, kwargs={"result": result})
    thread2 = Thread(target=t2, kwargs={"result": result})
    thread3 = Thread(target=t3, kwargs={"result": result})

    thread1.start()
    thread2.start()
    thread3.start()

    thread1.join()
    thread2.join()
    thread3.join()

    ### Parallel Execution Ends ###

    order_id1 = result["1"].json().get("orderId")
    status_code1 = result["1"].status_code

    order_id2 = result["2"].json().get("orderId")
    status_code2 = result["2"].status_code

    order_id3 = result["3"].json().get("orderId")
    status_code3 = result["3"].status_code

    # check if all orders were placed successfully & were assigned expected set of orderIds
    if {status_code1, status_code2, status_code3}!={201} or {order_id1, order_id2, order_id3}!={1000, 1001, 1002}:
        return 'Fail'


    result = {}

    ## Parallel Execution Begins ###
    thread1 = Thread(target=t4, kwargs={"result": result})
    thread2 = Thread(target=t5, kwargs={"result": result})

    thread1.start()
    thread2.start()

    thread1.join()
    thread2.join()

    ### Parallel Execution Ends ###

    status_code1 = result["1"].status_code

    status_code2 = result["2"].status_code


    # check if all signIn requests were correctly handled
    if {status_code1, status_code2}!={HTTPStatus.CREATED}:
        return "Fail"

    
    # Check status of order 1000
    http_response = requests.get(
        f"http://localhost:8081/order/1000")

    if(http_response.status_code != HTTPStatus.OK):
        return 'Fail'

    res_body = http_response.json()
    
    agent_id1 = res_body.get("agentId")
    order_status1 = res_body.get("status")

    # Check status of order 1001
    http_response = requests.get(
        f"http://localhost:8081/order/1001")

    res_body = http_response.json()

    agent_id2 = res_body.get("agentId")
    order_status2 = res_body.get("status")

    # Check status of order 1002
    http_response = requests.get(
        f"http://localhost:8081/order/1002")

    res_body = http_response.json()

    agent_id3 = res_body.get("agentId")
    order_status3 = res_body.get("status")

    # check if only orders 1000 and 1001 are assigned and 1002 is unassigned
    if {order_status1, order_status2} != {"assigned"} or order_status3!="unassigned":
        return "Fail"

    # check if agentId's were correctly assigned to orders
    if {agent_id1, agent_id2}!={201, 202} or agent_id3!=-1:
        return "Fail" 


    # check status of agents 
    # Check status of agent 201
    http_response = requests.get(
        f"http://localhost:8081/agent/201")

    status_code1 = http_response.status_code
    agent_status1 = http_response.json().get("status")

    # Check status of agent 202
    http_response = requests.get(
        f"http://localhost:8081/agent/202")

    status_code2 = http_response.status_code
    agent_status2 = http_response.json().get("status")

    if not (status_code1==200 and status_code2==200):
        return 'Fail'
    
    if not (agent_status1==agent_status2 and agent_status2=='unavailable'):
        return 'Fail'

    return 'Pass'


if __name__ == "__main__":

    print(test())

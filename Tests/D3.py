from http import HTTPStatus
from threading import Thread
import requests

# agent 201 is available, concurrent signout & requestOrder requests come
# Only 2 valid cases should happen
# 1: 201 is signed out & order is unassigned
# 2: 201 becomes unavailable & order is assigned to agent 201

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

    # Customer 301 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 1, "qty": 3})

    result["1"] = http_response


def t2(result):  # Second concurrent request

    # agent 201 requests signout
    http_response = requests.post(
        "http://localhost:8081/agentSignOut", json={"agentId": 201})

    result["2"] = http_response


def test():

    result = {}

    # Reinitialize Restaurant service
    http_response = requests.post("http://localhost:8080/reInitialize")

    # Reinitialize Delivery service
    http_response = requests.post("http://localhost:8081/reInitialize")

    # Reinitialize Wallet service
    http_response = requests.post("http://localhost:8082/reInitialize")

    # Agent 201 sign in
    http_response = requests.post(
        "http://localhost:8081/agentSignIn", json={"agentId": 201})

    if(http_response.status_code != HTTPStatus.CREATED):
        return 'Fail'

    ### Parallel Execution Begins ###
    thread1 = Thread(target=t1, kwargs={"result": result})
    thread2 = Thread(target=t2, kwargs={"result": result})

    thread1.start()
    thread2.start()

    thread1.join()
    thread2.join()

    ### Parallel Execution Ends ###

    order_id = result["1"].json().get("orderId")
    status_code1 = result["1"].status_code

    status_code2 = result["2"].status_code

    if {status_code2, status_code1}!= {HTTPStatus.CREATED} or order_id!=1000:
        return "Fail"

    # Check status of order
    http_response = requests.get(
        f"http://localhost:8081/order/1000")

    if(http_response.status_code != HTTPStatus.OK):
        return 'Fail'

    res_body = http_response.json()

    assigned_agent = res_body.get("agentId")
    order_status = res_body.get("status")

    # Check status of agent 201
    http_response = requests.get(
        f"http://localhost:8081/agent/201")

    res_body = http_response.json()

    agent_status = res_body.get("status")

    if agent_status == "unavailable":
        if not (order_status=="assigned" and assigned_agent==201):
            return 'Fail'
    else:
        if agent_status!="signed-out":
            return 'Fail'
        if not (order_status=='unassigned' and assigned_agent==-1):
            return 'Fail'

    return 'Pass'


if __name__ == "__main__":

    print(test())

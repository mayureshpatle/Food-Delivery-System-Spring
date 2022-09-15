from http import HTTPStatus
import json
from threading import Thread
import requests

# Sending the following sequence of requests
# 1. Concurrent /agentSignIn (for agent 201) & /requestOrder (will create order 1000)
# 2. /requestOrder (will create order 1001)
# 3. /orderDelivered (for order 1000)
# 4. /orderDelivered (for order 1001)
# 5. /orderDelivered (for order 1000)
# 6. /agentSignOut (for agent 201)
# No matter if 201 was assigned to order 1000 during concurrent request or not,
# after execution of this sequence, both orders should be delivered & agent 201 should be signed out


# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

    # agent 201 signs in
    http_response = requests.post(
        "http://localhost:8081/agentSignIn", json={"agentId": 201})

    result["1"] = http_response


def t2(result):  # Second concurrent request

    # Customer 302 requests an order of item 1, quantity 3 from restaurant 101      -> order 1000
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 302, "restId": 101, "itemId": 1, "qty": 3})

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

    thread1.start()
    thread2.start()

    thread1.join()
    thread2.join()

    ### Parallel Execution Ends ###

    status_code1 = result["1"].status_code

    status_code2 = result["2"].status_code

    if (status_code1 != status_code2 and status_code2 != HTTPStatus.CREATED) or result['2'].json().get('orderId')!=1000:
        return "Fail"

    # Customer 301 requests an order of item 1, quantity 3 from restaurant 101      -> order 1001
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 1, "qty": 3})

    if http_response.status_code!=201 or http_response.json().get("orderId")!=1001:
        return 'Fail'

    http_response = requests.post("http://localhost:8081/orderDelivered", json={"orderId": 1000})
    if http_response.status_code!=201:
        return 'Fail'

    http_response = requests.post("http://localhost:8081/orderDelivered", json={"orderId": 1001})
    if http_response.status_code!=201:
        return 'Fail'

    http_response = requests.post("http://localhost:8081/orderDelivered", json={"orderId": 1000})
    if http_response.status_code!=201:
        return 'Fail'

    http_response = requests.post("http://localhost:8081/agentSignOut", json={"agentId": 201})
    if http_response.status_code!=201:
        return 'Fail'

    http_response = requests.get("http://localhost:8081/agent/201")
    status_code1 = http_response.status_code
    agent_status = http_response.json().get("status")

    http_response = requests.get("http://localhost:8081/order/1000")
    status_code2 = http_response.status_code
    order_status1 = http_response.json().get("status")
    agentId1 = http_response.json().get("agentId")

    http_response = requests.get("http://localhost:8081/order/1001")
    status_code3 = http_response.status_code
    order_status2 = http_response.json().get("status")
    agentId2 = http_response.json().get("agentId")

    if {status_code1, status_code2, status_code3}!={200}:
        return 'Fail'
    
    if {agentId1, agentId2} != {201}:
        return 'Fail'   

    if agent_status!="signed-out" or  {order_status1, order_status2}!={"delivered"}:
        return 'Fail' 

    return 'Pass'


if __name__ == "__main__":

    print(test())

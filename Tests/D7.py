from http import HTTPStatus
from threading import Thread
import requests

# request 2 orders concurrently, out of which only 1 should be placed
# available quantity is 10, so asking for 2 orders with quantity 8

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

    # Customer 301 requests an order of item 1, quantity 8 from restaurant 101
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 1, "qty": 8})

    result["1"] = http_response


def t2(result):  # Second concurrent request

    # Customer 302 requests an order of item 1, quantity 8 from restaurant 101
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 302, "restId": 101, "itemId": 1, "qty": 8})

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

    if status_code1==status_code2 or {status_code1,status_code2}!={410, 201}:
        return 'Fail'

    http_response = requests.get("http://localhost:8081/order/1000")
    status_code1 = http_response.status_code

    http_response = requests.get("http://localhost:8081/order/1001")
    status_code2 = http_response.status_code

    if status_code1!=200 or status_code2!=404:
        return 'Fail'

    return 'Pass'


if __name__ == "__main__":

    print(test())

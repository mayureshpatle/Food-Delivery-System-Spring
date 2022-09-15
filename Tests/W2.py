from http import HTTPStatus
from threading import Thread
import requests


# Check if concurrent requests to '/addBalance' is updating the data consistently

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

    # add balance of 500 to wallet of customer 301

    http_response = requests.post(
        "http://localhost:8082/addBalance", json={"custId": 301, "amount": 500}
    )

    result["1"] = http_response


def t2(result):  # Second concurrent request

    # add balance of 600 to wallet of customer 301

    http_response = requests.post(
        "http://localhost:8082/addBalance", json={"custId": 301, "amount": 600}
    )

    result["2"] = http_response


def test():

    result = {}

    # Reinitialize Restaurant service

    http_response = requests.post("http://localhost:8082/reInitialize")

    # ## Parallel Execution Begins ###

    thread1 = Thread(target=t1, kwargs={"result": result})
    thread2 = Thread(target=t2, kwargs={"result": result})

    thread1.start()
    thread2.start()

    thread1.join()
    thread2.join()

    # ## Parallel Execution Ends ###


    # check whether the wallet is updated by concurrent 
    # exection as expected by serial execution
    
    http_response = requests.get("http://localhost:8082/balance/301")

    if http_response.json().get("balance") != 3100:
        return "Fail"

    return "Pass"


if __name__ == "__main__":

    print(test())

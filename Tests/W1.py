from http import HTTPStatus
from threading import Thread
import requests


# Check if concurrent requests to '/deductBalance' is updating the data consistently

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

    # deduct balance of 500 from wallet of customer 301

    http_response = requests.post(
        "http://localhost:8082/deductBalance", json={"custId": 301, "amount": 500}
    )

    result["1"] = http_response


def t2(result):  # Second concurrent request

    # deduct balance of 600 from wallet of customer 301

    http_response = requests.post(
        "http://localhost:8082/deductBalance", json={"custId": 301, "amount": 600}
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

    status_code1 = result["1"].status_code

    status_code2 = result["2"].status_code

    if status_code1 != 201 or status_code2 != 201:
        return "Fail"

    # check whether the wallet is updated by concurrent 
    # exection as expected by serial execution
    
    http_response = requests.get("http://localhost:8082/balance/301")

    if http_response.json().get("balance") != 900:
        return "Fail"

    return "Pass"


if __name__ == "__main__":

    print(test())

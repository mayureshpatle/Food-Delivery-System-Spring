#!/bin/sh

minikube start


eval $(minikube docker-env)

# build images

docker build -t vivek-restaurant ./Restaurant
docker build -t vivek-wallet ./Wallet
docker build -t vivek-delivery ./Delivery
docker build -t vivek-database ./Database

# restaurant

minikube kubectl -- create deployment vivek-restaurant --image=vivek-restaurant
minikube kubectl -- patch deployment vivek-restaurant -p '{"spec":{"template":{"spec":{"containers":[{
"name": "vivek-restaurant",
"image": "vivek-restaurant",
"resources": {},
"terminationMessagePath": "/dev/termination-log",
"terminationMessagePolicy": "File",
"imagePullPolicy": "Never"
}]}}}}'
minikube kubectl -- expose deployment vivek-restaurant --port=8080

# wallet

minikube kubectl -- create deployment vivek-wallet --image=vivek-wallet
minikube kubectl -- patch deployment vivek-wallet -p '{"spec":{"template":{"spec":{"containers":[{
"name": "vivek-wallet",
"image": "vivek-wallet",
"resources": {},
"terminationMessagePath": "/dev/termination-log",
"terminationMessagePolicy": "File",
"imagePullPolicy": "Never"
}]}}}}'
minikube kubectl -- expose deployment vivek-wallet --port=8080

#database

minikube kubectl -- create deployment vivek-database --image=vivek-database
minikube kubectl -- patch deployment vivek-database -p '{"spec":{"template":{"spec":{"containers":[{
"name": "vivek-database",
"image": "vivek-database",
"resources": {},
"terminationMessagePath": "/dev/termination-log",
"terminationMessagePolicy": "File",
"imagePullPolicy": "Never"
}]}}}}'
minikube kubectl -- expose deployment vivek-database --port=8080

# delivery

minikube kubectl -- create deployment vivek-delivery --image=vivek-delivery
minikube kubectl -- autoscale deployment vivek-delivery --cpu-percent=10 --min=1 --max=3

minikube kubectl -- patch deployment vivek-delivery -p '{"spec":{"template":{"spec":{"containers":[{
"name": "vivek-delivery",
"image": "vivek-delivery",
"resources": {},
"terminationMessagePath": "/dev/termination-log",
"terminationMessagePolicy": "File",
"imagePullPolicy": "Never"
}]}}}}'

minikube kubectl -- expose deployment vivek-delivery --type=LoadBalancer --port=8080

sleep 30

# port forwarding
minikube kubectl port-forward deployment/vivek-restaurant 8080:8080 &
minikube kubectl port-forward deployment/vivek-delivery 8081:8080 &
minikube kubectl port-forward deployment/vivek-wallet 8082:8080 &
minikube kubectl port-forward deployment/vivek-database 8083:8080 &

minikube tunnel &

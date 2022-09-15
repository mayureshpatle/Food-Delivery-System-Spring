#!/bin/sh

#delete all database tables

echo "dropping tables...."
curl -X POST http://localhost:8083/dropAll

#delete all services

minikube kubectl delete service vivek-restaurant
minikube kubectl delete service vivek-wallet
minikube kubectl delete service vivek-database
minikube kubectl delete service vivek-delivery

#delete all deployments

minikube kubectl delete deployment vivek-restaurant
minikube kubectl delete deployment vivek-wallet
minikube kubectl delete deployment vivek-database
minikube kubectl delete deployment vivek-delivery


#stop minikube

minikube stop

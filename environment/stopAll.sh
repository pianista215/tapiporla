#!/bin/bash
#TODO: Stop only project dockers
sudo docker rm -f $(sudo docker ps -a -q)

#!/bin/bash
#TODO: Stop only project dockers
sudo docker stop $(sudo docker ps -a -q)

#!/bin/bash

echo -e "\nDelete Ibex35:\n"
curl -XDELETE -u elastic:changeme http://localhost:9200/ibex35

echo -e "\nDelete BBVA:\n"
curl -XDELETE -u elastic:changeme http://localhost:9200/bbva

echo -e "\nFinished\n"

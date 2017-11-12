#!/bin/bash

echo "Launching Ibex35 crawler Docker:"
sudo docker run --rm -p 9080:9080 -d -v /tmp/logs/:/scrapyrt/project/logs pianista215/ibex35:0.1.0

echo "Launching ES Docker:"
sudo mkdir -p /data/elasticsearch
sudo chown 1000:1000 /data/elasticsearch
sudo docker run --rm -p 9200:9200 -p 9300:9300 -v /data/elasticsearch:/usr/share/elasticsearch/data -d -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:5.6.4

echo "Launching Kibana Docker:"
sudo docker run --rm --net=host -d -e "ELASTICSEARCH_URL=http://127.0.0.1:9200" docker.elastic.co/kibana/kibana:5.6.4

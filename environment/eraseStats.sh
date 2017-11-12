#!/bin/bash
curl -XDELETE -u elastic:changeme 'http://localhost:9200/ibex35/stats/_query' -d '{
    "query" : { 
        "match_all" : {}
    }
}'

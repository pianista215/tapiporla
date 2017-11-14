#!/bin/bash
curl -XPOST -u elastic:changeme 'http://localhost:9200/ibex35/stats/_delete_by_query' -d '{
    "query" : { 
        "match_all" : {}
    }
}'

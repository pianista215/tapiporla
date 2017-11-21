#!/bin/bash

indices=`curl -XGET http://elastic:changeme@localhost:9200/_mapping?pretty=true | jq 'to_entries | .[] | {(.key): .value.mappings | keys}' | jq 'to_entries | .[] | select(.value | index("historic")) | .key'`
sanitized=`echo "$indices" | tr -d '"'`

for index in $sanitized
do
  echo "Deleting $index" 
  curl -XDELETE -u elastic:changeme http://localhost:9200/$index
done

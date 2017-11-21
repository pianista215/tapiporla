#!/bin/bash

if [ "$#" -ne 1 ]; then
   echo "Usage: backupESHistoric.sh <path>"
   exit -1
fi

DATE=`date +%Y-%m-%d`
FINAL_PATH="$1/$DATE/"

echo "Generating backup folder: $FINAL_PATH"
mkdir -p $FINAL_PATH

#Retrieve the indices that have "historic" data, and backup them in the provided path using elasticdump

indices=`curl -XGET http://elastic:changeme@localhost:9200/_mapping?pretty=true | jq 'to_entries | .[] | {(.key): .value.mappings | keys}' | jq 'to_entries | .[] | select(.value | index("historic")) | .key'`
sanitized=`echo "$indices" | tr -d '"'`

echo "Indices with historic data:"
echo "$sanitized"

for index in $sanitized
do
   echo "Backuping mappings: $index"
   docker run --net=host -v $FINAL_PATH:/tmp --rm -ti taskrabbit/elasticsearch-dump --input=http://elastic:changeme@127.0.0.1:9200/$index/historic --output=/tmp/${index}_mapping.json --type=mapping 
   echo "Backuping data: $index"
   docker run --net=host -v $FINAL_PATH:/tmp --rm -ti taskrabbit/elasticsearch-dump --input=http://elastic:changeme@127.0.0.1:9200/$index/historic --output=/tmp/${index}_data.json --type=data
done

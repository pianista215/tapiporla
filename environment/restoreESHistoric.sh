#!/bin/bash

if [ "$#" -ne 1 ]; then
   echo "Usage: restoreESHistoric.sh <path>"
   exit -1
fi

BACKUP_PATH=$1

indices=$(basename -s _mapping.json `find $BACKUP_PATH -maxdepth 1 -name *_mapping.json`)

for index in $indices
do
   echo -e "\nRestoring mappings: $index \n"
   docker run --net=host -v $BACKUP_PATH:/tmp --rm -ti taskrabbit/elasticsearch-dump --output=/tmp/${index}_mapping.json --output=http://elastic:changeme@127.0.0.1:9200/$index --type=mapping
   echo -e "\nRestoring data: $index \n"
   docker run --net=host -v $BACKUP_PATH:/tmp --rm -ti taskrabbit/elasticsearch-dump --input=/tmp/${index}_data.json --output=http://elastic:changeme@127.0.0.1:9200/$index/historic --type=data
done

#!/bin/bash

flooding_db_image_name="flooding-db-image"
flooding_db_name="flooding-db"
flooding_db_password="password"

echo "Killing old container if necessary"
if [[ $(docker container ls --filter "NAME=${flooding_db_name}") != "Error*" ]]; then
  docker rm -f ${flooding_db_name}
fi

echo ""

echo "Killing old image ${flooding_db_image_name} if necessary and re-creating it"
if [[ $(docker image ls --filter "reference=flooding-db-image") != "" ]]; then
  docker image rm -f flooding-db-image
fi
docker build -t ${flooding_db_image_name}:latest -e PG_USER=postgres -e PG_DB=${flooding_db_name} -e PG_PSWRD=${flooding_db_password} .

echo ""

echo "Creating postgres DB with docker using DB name ${flooding_db_name} and password ${flooding_db_password} for user postgres"
#docker run --name ${flooding_db_name} -p 5432:5432 -e PG_USER=postgres -e PG_DB="${flooding_db_name}" -e PG_PSWRD="${flooding_db_password}" -d ${flooding_db_image_name}
docker run --name ${flooding_db_name} -p 5432:5432 -d ${flooding_db_image_name}

#echo "BOB"
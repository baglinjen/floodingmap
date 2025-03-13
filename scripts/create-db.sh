#!/bin/bash

flooding_db_image_name="flooding-db-image"
flooding_db_name="flooding-db"
flooding_db_password="password"

echo "Killing old container"
docker rm -f ${flooding_db_name}

echo ""

echo "Building image if necessary"
docker build -t ${flooding_db_image_name}:latest .

echo ""

echo "Creating postgres DB with docker using DB name ${flooding_db_name} and password ${flooding_db_password} for user postgres"
docker run --name ${flooding_db_name} -p 5433:5432 -e POSTGRES_USER=postgres -e POSTGRES_DB=${flooding_db_name} -e POSTGRES_PASSWORD=${flooding_db_password} -d ${flooding_db_image_name}

echo ""

echo "Awaiting boots to be over"

while [[ "$(docker exec -it ${flooding_db_name} pg_isready -U postgres)" == *"accepting connections"* ]]; do
#  echo "$(docker exec -it ${flooding_db_name} pg_isready -U postgres)"
  sleep 0.1
done;

echo "First boot over"

while [[ "$(docker exec -it ${flooding_db_name} pg_isready -U postgres)" == *"no response"* ]]; do
#  echo "$(docker exec -it ${flooding_db_name} pg_isready -U postgres)"
  sleep 0.1
done;

echo "First shutdown"

while [[ "$(docker exec -it ${flooding_db_name} pg_isready -U postgres)" == *"accepting connections"* ]]; do
#  echo "$(docker exec -it ${flooding_db_name} pg_isready -U postgres)"
  sleep 0.1
done;

echo "Second boot over"

sleep 1

echo ""

echo "Pre-seeding database"

PGPASSWORD=${flooding_db_password}
cat init.sql | docker exec -i ${flooding_db_name} psql -h localhost -p 5432 -U postgres -f-
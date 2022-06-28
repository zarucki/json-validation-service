#!/bin/bash

PORT=8080

echo -e "Uploading schema"
cat ./src/test/resources/example-schema.json
echo -e "\nPOST http://localhost:$PORT/schema/config-schema"
curl "http://localhost:$PORT/schema/config-schema" -X POST -d @./src/test/resources/example-schema.json

echo -e "\n\nGetting uploaded schema"
echo -e "GET http://localhost:$PORT/schema/config-schema"
curl "http://localhost:$PORT/schema/config-schema" -X GET

echo -e "\n\nValidating valid json which contains nulls"
cat ./src/test/resources/example-object-valid.json
echo -e "\nPOST http://localhost:$PORT/validate/config-schema"
curl "http://localhost:$PORT/validate/config-schema" -X POST -d @./src/test/resources/example-object-valid.json

echo -e "\n\nValidating invalid json"
cat ./src/test/resources/example-object-invalid.json
echo -e "\nPOST http://localhost:$PORT/validate/config-schema"
curl "http://localhost:$PORT/validate/config-schema" -X POST -d @./src/test/resources/example-object-invalid.json
echo


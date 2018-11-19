#!/bin/bash
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh

function findScript {
SCRIPT_ID=$(curl --silent\
     --header "Accept: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=1.0, protocol=1.0" \
     --get\
     --data-urlencode '_queryFilter=name eq "'"$1"'"'\
     --data-urlencode '_fields=_id'\
     "$SERVER_URI/json/scripts/" | jq -r .result[0]._id)
#     if [ "$SCRIPT_ID" != "null" ]; then
#        echo "  Script found: name=$1, id=$SCRIPT_ID"
#     else 
#        echo "  Script NOT found: name=$1"
#     fi
}

function deleteScript {
echo "Deleting script $1"
    findScript "$1"
    if [ "null" == "$SCRIPT_ID" ]; then
        echo "  Script not found"
        return;
    fi

RESPONSE=$(curl --silent\
     --header "Accept: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=1.0, protocol=1.0" \
     --request DELETE\
     "$SERVER_URI/json/scripts/$SCRIPT_ID" | jq -r .)

    if [ "{}" == "$RESPONSE" ]; then
        echo "  Success!"
    else 
        echo $RESPONSE
    fi
}

## Creates a new script or updates an existing one
function replaceScript {

    ## Parameters:
    SCRIPT_NAME=$1 
    SCRIPT_TYPE=$2
    SCRIPT_LANGUAGE=$3
    SCRIPT_FILENAME=$4
    SCRIPT_DESCRIPTION=$5

    findScript "$SCRIPT_NAME"

    if [ "$SCRIPT_ID" = "null" ]; then
       METHOD=POST
       URI="$SERVER_URI/json/scripts/?_action=create"
       echo "Creating script $SCRIPT_NAME"
       ACTION="created"
    else
       METHOD=PUT
       URI="$SERVER_URI/json/scripts/$SCRIPT_ID"
       echo "Replacing script $SCRIPT_NAME"
       ACTION="updated"
    fi

    SCRIPT_BASE64=$(cat "$SCRIPT_DIR/$SCRIPT_FILENAME" | base64Encode)
    SAMPLE=$(echo "$SCRIPT_BASE64"|head -c 30)
    echo "  Script Base64 encoded: $SAMPLE ..." 

    SCRIPT_ID=$(curl --silent\
                 --request $METHOD\
                 --header "Content-Type: application/json"\
                 --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
                 --header "Accept-API-Version: resource=1.0, protocol=1.0" \
                 --data '{
                            "name": "'"$SCRIPT_NAME"'",
                            "description": "'"$SCRIPT_DESCRIPTION"'",
                            "script": "'"$SCRIPT_BASE64"'",
                            "language": "'"$SCRIPT_LANGUAGE"'",
                            "context": "'"$SCRIPT_TYPE"'"
                         }'\
         "$URI"|jq -r ._id)

    if [ "$SCRIPT_ID" != "null" ]; then
        echo "  Script $ACTION successfully: $SCRIPT_ID"
    fi 
}

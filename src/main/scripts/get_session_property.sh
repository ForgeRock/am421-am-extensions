#!/bin/bash
#Gets the given session property of the queriedToken
SCRIPT_DIR="$( dirname "$( which "$0" )" )"

SERVER_URI="http://login.example.com:18080/am"

function echo_usage {
    (>&2 echo "Usage: $0 propertyName queriedToken [performerToken]"
    )
    exit 1
}

PROPERTY=$1
QUERIED_TOKEN=$2
PERFORMER_TOKEN=$3

if [ -z ${3+x} ]; then
    PERFORMER_TOKEN=$($SCRIPT_DIR/authenticate.sh amadmin cangetinam)
fi

if [[ -z ${1+x} || -z ${2+x} ]]; then
    echo_usage
fi

curl -s -X POST\
    --header "iPlanetDirectoryPro: $PERFORMER_TOKEN"\
    --header "Content-Type: application/json"\
    --header "Accept-API-Version: resource=1.1, protocol=1.0" \
    --data '{
               "properties" : [ "'"$PROPERTY"'" ]
            }'\
    "$SERVER_URI/json/sessions/?_action=getProperty&tokenId=$QUERIED_TOKEN" | jq -r ".$PROPERTY"
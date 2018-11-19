#!/bin/bash
#Authenticate with OpenAM
SERVER_URI="http://login.example.com:18080/am"

USERNAME=$1
PASSWORD=$2

function echo_usage {
    (>&2 echo "Usage: $0 username password [chain name]"
         echo "       Only those use cases are supported, where OpenAM"
         echo "       returns immediately with the tokenId"
    )
}

if [[ -z ${1+x} || -z ${2+x} ]]; then
    echo_usage
    exit 1
fi

if [ -z ${3+x} ]; then
    AUTH_PARAMS=""
else
    CHAIN=$3
    AUTH_PARAMS="authIndexType=service&authIndexValue=$CHAIN"
fi

RESPONSE=$(\
    curl -s -X POST \
         --header "X-OpenAM-Username: $USERNAME" \
         --header "X-OpenAM-Password: $PASSWORD" \
         --header "Content-Type: application/json" \
         --header "Accept-API-Version: resource=1.1, protocol=1.0" \
         --data "{}" \
         "$SERVER_URI/json/authenticate?$AUTH_PARAMS")

if [[ $(echo "$RESPONSE" | jq -r ".authId != null") = "true" ]]; then
    (>&2 echo "Multi-step authentication is not supported, response is: $RESPONSE")
    echo_usage
else
    TOKEN=$(echo "$RESPONSE" | jq -r .tokenId)
    if [[ $TOKEN == null ]]; then
        (>&2 echo "$RESPONSE")
        echo_usage
    else
        echo $TOKEN
    fi
fi
#!/bin/bash
#Evaluate policies in the given application (policy set)
SERVER_URI="http://login.example.com:18080/am"

function echo_usage {
    (>&2 echo "Usage: $0 application resource evaluatorToken [subjectToken]"
    )
    exit 1
}

APPLICATION=$1
RESOURCE=$2
EVALUATOR_TOKEN=$3
SUBJECT=""

if [[ -z ${1+x} || -z ${2+x} || -z ${3+x} ]]; then
    echo_usage
fi

if [ -n "$4" ]; then
    SUBJECT_TOKEN=$4
    SUBJECT='"subject" : { "ssoToken" : "'"$SUBJECT_TOKEN"'"},'
else
    SUBJECT=''
fi

curl -s -X POST\
    --header "iPlanetDirectoryPro: $EVALUATOR_TOKEN"\
    --header "Content-Type: application/json"\
    --header "Accept-API-Version: resource=2.0, protocol=1.0" \
    --data '{
               "application" : "'"$APPLICATION"'",
               '"$SUBJECT"'
               "resources" : [ "'"$RESOURCE"'" ]
            }'\
    "$SERVER_URI/json/policies?_action=evaluate" | jq -r .

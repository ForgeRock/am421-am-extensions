#!/bin/bash

#[[ $ADMIN_TOKEN == "" ]] && echo "Error: Missing tokenId parameter" && exit ;

SERVER_URI="http://login.example.com:18080/am"

function findPolicies {
curl --silent\
     --header "Accept: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=2.0, protocol=1.0" \
     --get\
     --data-urlencode '_queryFilter=applicationName eq "'"$1"'"'\
     --data-urlencode '_sortKeys=name'\
     --data-urlencode '_fields=name'\
     "$SERVER_URI/json/policies/" | jq -r ".result[] | .name"
}

function urlEncode {
    echo -n "$1" | perl -pe's/([^-_.~A-Za-z0-9])/sprintf("%%%02X", ord($1))/seg'
}

function deletePolicy {
echo "Deleting policy $1"
ENCODED=$(urlEncode $1);

RESPONSE=$(curl --silent\
     --header "Accept: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=2.0, protocol=1.0" \
     --request DELETE\
     "$SERVER_URI/json/policies/$ENCODED" | jq -r .)
     if [ "{}" = "$RESPONSE"  ]; then
        echo "  Success!"
     else
        echo "  "$(echo $RESPONSE | jq -r .reason)
     fi
}

function deletePolicySet {
IFS=$(echo -en "\n\b")
for policyName in $(findPolicies "$1"); do
    deletePolicy $policyName
done

echo "Deleting policy set $1"

ENCODED=$(urlEncode $1);

RESPONSE=$(curl --silent\
     --header "Accept: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=1.0, protocol=1.0" \
     --request DELETE\
     "$SERVER_URI/json/applications/$ENCODED" | jq -r .)

     if [ "{}" = "$RESPONSE"  ]; then
        echo "  Success!"
     else
        echo "  "$(echo $RESPONSE | jq -r .reason)
     fi
}

function findPolicySetsByResourceTypeUUID {
curl --silent\
     --header "Accept: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=1.0, protocol=1.0" \
     --get\
     --data-urlencode '_queryFilter=true'\
     --data-urlencode '_sortKeys=name'\
     --data-urlencode '_fields=name,resourceTypeUuids'\
     "$SERVER_URI/json/applications/" | jq -r '.result|map(select(contains({"resourceTypeUuids":["'"$1"'"]})))'
}


function getResourceTypeUUID {
curl --silent\
     --header "Accept: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=1.0, protocol=1.0" \
     --get\
     --data-urlencode '_queryFilter=name eq "'"$1"'"'\
     --data-urlencode '_sortKeys=name'\
     --data-urlencode '_fields=uuid'\
     "$SERVER_URI/json/resourcetypes/"| jq -r ".result[] | .uuid"
}

function deleteResourceType {
    IFS=$(echo -en "\n\b")
    for resourceTypeUUID in $(getResourceTypeUUID $1); do
        deleteResourceTypeByUUID $1 $resourceTypeUUID
    done
}

function deleteResourceTypeByUUID {
echo "Deleting resource type $1 (uuid: $2)"

RESPONSE=$(curl --silent\
     --header "Accept: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=1.0, protocol=1.0" \
     --request DELETE\
     "$SERVER_URI/json/resourcetypes/$2" | jq -r .)

     if [ "{}" = "$RESPONSE"  ]; then
        echo "  Success!"
     else
        echo "  "$(echo $RESPONSE | jq -r .reason)
     fi
}

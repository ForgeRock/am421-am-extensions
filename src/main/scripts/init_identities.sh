#!/bin/bash
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh
tomcat_start
obtain_admin_token

function replace_user {
    SUCCESS=$(curl --silent \
         --request DELETE \
         --header "iPlanetDirectoryPro: $ADMIN_TOKEN" \
         --header "Accept-API-Version: resource=2.0, protocol=1.0" \
    "http://login.example.com:18080/am/json/users/$1" | jq -r .success)

    if [ "$SUCCESS" = "true" ]; then
         echo "deleted user: $1"
    fi

    CREATED_NAME=$(curl --silent \
         --request PUT \
         --header "iPlanetDirectoryPro: $ADMIN_TOKEN" \
         --header "Content-Type: application/json" \
         --header "Accept-API-Version: resource=2.0, protocol=1.0" \
         --header "If-None-Match: *" \
         --data "$2" \
    "http://login.example.com:18080/am/json/users/$1" | jq -r .username)

    if [ "$CREATED_NAME" != "null" ]; then
         echo "created user: $CREATED_NAME"
    fi
}

function replace_group {
    SUCCESS=$(curl --silent \
         --request DELETE \
         --header "iPlanetDirectoryPro: $ADMIN_TOKEN" \
         --header "Accept-API-Version: resource=2.0, protocol=1.0" \
    "http://login.example.com:18080/am/json/groups/$1" | jq -r .success)


    if [ "$SUCCESS" = "true" ]; then
         echo "deleted group: $1"
    fi

    CREATED_NAME=$(curl --silent \
         --request POST \
         --header "iPlanetDirectoryPro: $ADMIN_TOKEN" \
         --header "Content-Type: application/json" \
         --header "Accept-API-Version: resource=2.0, protocol=1.0" \
         --data "$2" \
    "http://login.example.com:18080/am/json/groups/?_action=create" | jq -r .username)

    if [ "$CREATED_NAME" != "null" ]; then
         echo "created group: $CREATED_NAME"
    fi
}

function add_privileges {
   echo "Adding EntitlementRestAccess to All Authenticated Users"
   echo "Adding SessionPropertyModifyAccess to the SessionPropertyReader group"
   rm -f /tmp/ssoadm_script
   cat <<END > /tmp/ssoadm_script
add-privileges -e / -t role  -i "All Authenticated Users" -g EntitlementRestAccess
add-privileges -e / -t group -i SessionPropertyReader -g SessionPropertyModifyAccess 
END
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script  | grep -v "^$"
   rm -f /tmp/ssoadm_script
}

TO=$(jq -r "length - 1" < $SCRIPT_DIR/users.json)

for i in $(seq 0 $TO); do
    USER=$(jq -r ".[$i]" < $SCRIPT_DIR/users.json)
    NAME=$(echo "$USER" | jq -r .username)
    replace_user "$NAME" "$USER"
done

TO=$(jq -r "length - 1" < $SCRIPT_DIR/groups.json)

for i in $(seq 0 $TO); do
    GROUP=$(jq -r ".[$i]" < $SCRIPT_DIR/groups.json)
    NAME=$(echo "$GROUP" | jq -r .username)
    replace_group "$NAME" "$GROUP"
done

add_privileges

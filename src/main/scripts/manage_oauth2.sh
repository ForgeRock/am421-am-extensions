#!/bin/bash
# FR-421 - OpenAM OAuth2 Setup script
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh

function remove_oauth2 {
   echo "Deleting OAuth2Provider, UMAProvider and OAuth2 Client (named ContactList)"
   echo "delete-agents --realm / --agentnames ContactList" > /tmp/ssoadm_script
   echo "delete-agents --realm / --agentnames ContactListBackend" >> /tmp/ssoadm_script
   echo "remove-svc-realm --realm / --servicename OAuth2Provider" >> /tmp/ssoadm_script
   echo "remove-svc-realm --realm / --servicename UmaProvider" >> /tmp/ssoadm_script
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script | grep -v "^$"
   rm -f /tmp/ssoadm_script
}

function remove_oauth2_client {
   CLIENT_NAME=$1
   echo "Removing OAuth2 Client (named $CLIENT_NAME)"
   echo "delete-agents --realm / --agentnames $CLIENT_NAME" > /tmp/ssoadm_script
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script | grep -v "^$"
   rm -f /tmp/ssoadm_script
}

function replace_oauth2_client {
   CLIENT_NAME=$1
   CLIENT_PROPERTIES=$2
   echo "Replacing OAuth2 Client (named $CLIENT_NAME)"
   echo "delete-agents --realm / --agentnames $CLIENT_NAME" > /tmp/ssoadm_script
   echo "create-agent --realm / --agentname $CLIENT_NAME --agenttype OAuth2Client --datafile $SCRIPT_DIR/$CLIENT_PROPERTIES.properties" >> /tmp/ssoadm_script
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script | grep -v "^$"
   rm -f /tmp/ssoadm_script
}

function replace_oauth2_provider {
   echo "Replacing OAuth2Provider"
   echo "remove-svc-realm --realm / --servicename OAuth2Provider" > /tmp/ssoadm_script
   echo "add-svc-realm --realm / --servicename OAuth2Provider --datafile $SCRIPT_DIR/$1.properties" >> /tmp/ssoadm_script
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script | grep -v "^$"
   rm -f /tmp/ssoadm_script
}

function replace_uma_provider {
   echo "Replacing UmaProvider"
   echo "remove-svc-realm --realm / --servicename UmaProvider" > /tmp/ssoadm_script
   echo "add-svc-realm --realm / --servicename UmaProvider --datafile $SCRIPT_DIR/$1.properties" >> /tmp/ssoadm_script
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script | grep -v "^$"
   rm -f /tmp/ssoadm_script
}

case $1 in
    "lab01-end"|"lab02-end"|"lab03-end"|"lab04-end"|"lab05-end"|"lab06-end"|"lab07-end"|"lab08-end"|"lab09-end"|"lab10-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        remove_oauth2
        ;;
    "lab11-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        replace_oauth2_provider OAuth2Provider_lab11-end
        remove_oauth2_client   ContactListBackend
        replace_oauth2_client   ContactList OAuth2Client_ContactList_lab11-end
        ;;
    "lab12-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        replace_oauth2_provider OAuth2Provider_lab12-end
        replace_uma_provider    UMAProvider
        replace_oauth2_client   ContactList OAuth2Client_ContactList_lab12-end
        replace_oauth2_client   ContactListBackend OAuth2Client_ContactListBackend_lab12-end
        ;;
    *)
      echo "Usage: $0 labXX-end, where XX can be 01..12"
      echo "~/fr_scripts.config can override configuration variables"
esac

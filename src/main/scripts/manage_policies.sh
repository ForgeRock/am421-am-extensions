#!/bin/bash
# FR-421 - OpenAM OAuth2 Setup script
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh
source $SCRIPT_DIR/policy_functions.sh

function create_policies_REST {
    $SCRIPT_DIR/create_policies_ContactListREST.sh $ADMIN_TOKEN
}

function create_policies_Privileges {
    $SCRIPT_DIR/create_policies_ContactListPrivileges.sh $ADMIN_TOKEN
}


case $1 in
    "remove-rest" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        deletePolicySet ContactListREST
        deleteResourceType ContactListREST
        ;;
    "create-rest" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        create_policies_REST
        ;;
    "replace-rest")
        echo "####################"
        tomcat_start
        obtain_admin_token
        deletePolicySet ContactListREST
        deleteResourceType ContactListREST
        create_policies_REST
        ;;
    "remove-privileges" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        deletePolicySet ContactListPrivileges
        deleteResourceType ContactListPrivileges
        ;;
    "create-privileges" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        deletePolicySet ContactListPrivileges
        deleteResourceType ContactListPrivileges
        create_policies_Privileges
        ;;
    "replace-privileges" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        deletePolicySet ContactListPrivileges
        deleteResourceType ContactListPrivileges
        create_policies_Privileges
        ;;
    "remove-all"|"lab01-end"|"lab02-end"|"lab03-end"|"lab04-end"|"lab05-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        deletePolicySet ContactListREST
        deletePolicySet ContactListPrivileges
        deleteResourceType ContactListREST
        deleteResourceType ContactListPrivileges
        ;;
    "lab06-end"|"lab07-end"|"lab08-end"|"lab09-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        deletePolicySet ContactListREST
        deletePolicySet ContactListPrivileges
        deleteResourceType ContactListREST
        deleteResourceType ContactListPrivileges
        create_policies_REST
        ;;
    "replace-all"|"lab10-end"|"lab11-end"|"lab12-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        deletePolicySet ContactListREST
        deletePolicySet ContactListPrivileges
        deleteResourceType ContactListREST
        deleteResourceType ContactListPrivileges
        create_policies_REST
        create_policies_Privileges
        ;;
    *)
      echo "Usage: $0 labXX-end where XX can be 01..12"
      echo "~/fr_scripts.config can override configuration variables"
esac
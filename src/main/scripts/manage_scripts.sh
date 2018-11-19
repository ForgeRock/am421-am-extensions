#!/bin/bash
# FR-421 - OpenAM Scripts Setup script
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh
source $SCRIPT_DIR/script_functions.sh
source $SCRIPT_DIR/script_definitions.sh

case $1 in
    "lab01-end"|"lab02-end"|"lab03-end"|"lab04-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        deleteScript ResourceIsOwnedByCurrentUser
        deleteScript ServerInMaintenanceMode
        deleteScript "Reject Users with Disabled Profile"
        replace_TimeCheckServer_script
        replace_TimeCheckClient_script
        replace_OIDC_Claims_Script_original_script
        ;;
    "lab05-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        deleteScript ResourceIsOwnedByCurrentUser
        deleteScript ServerInMaintenanceMode
        replace_TimeCheckServer_script
        replace_TimeCheckClient_script
        replace_OIDC_Claims_Script_original_script
        replace_RejectUsersWithDisabledProfile_script
        ;;
    "lab06-end"|"lab07-end"|"lab08-end"|"lab09-end"|"lab10-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        replace_TimeCheckServer_script
        replace_TimeCheckClient_script
        replace_ResourceIsOwnedByCurrentUser_script
        replace_ServerInMaintenanceMode_script
        replace_OIDC_Claims_Script_original_script
        replace_RejectUsersWithDisabledProfile_script
        ;;
    "lab11-end"|"lab12-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        replace_TimeCheckServer_script
        replace_TimeCheckClient_script
        replace_ResourceIsOwnedByCurrentUser_script
        replace_ServerInMaintenanceMode_script
        replace_OIDC_Claims_Script_lab11end_script
        replace_RejectUsersWithDisabledProfile_script
        ;;
    *)
      echo "Usage: $0 labXX-end where XX can be 01..12"
      echo "~/fr_scripts.config can override configuration variables"
esac
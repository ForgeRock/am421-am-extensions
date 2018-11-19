#!/bin/bash
# FR-421 - OpenAM Scripts Setup script
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh
source $SCRIPT_DIR/script_functions.sh

function replace_TimeCheckServer_script {

    replaceScript "TimeCheck Server"\
                  "AUTHENTICATION_SERVER_SIDE"\
                  "GROOVY"\
                  "TimeCheck Server.groovy"\
                  "Authenticates successfully if the client side's time and the server side's time has no significant difference."
}

function replace_TimeCheckClient_script {

    replaceScript "TimeCheck Client"\
                  "AUTHENTICATION_CLIENT_SIDE"\
                  "JAVASCRIPT"\
                  "TimeCheck Client.js"\
                  "Captures the browser's current time and sends it to the server side script."
}

function replace_RejectUsersWithDisabledProfile_script {

    replaceScript "Reject Users with Disabled Profile"\
                  "AUTHENTICATION_SERVER_SIDE"\
                  "JAVASCRIPT"\
                  "RejectUsersWithDisabledProfile.js"\
                  "Authenticates successfully if the current user's profile is not disabled."
}

function replace_ResourceIsOwnedByCurrentUser_script {

    replaceScript "ResourceIsOwnedByCurrentUser"\
                  "POLICY_CONDITION"\
                  "JAVASCRIPT"\
                  "ResourceIsOwnedByCurrentUser.js"\
                  "Checks whether the current user is the owner of the current resource."
}

function replace_ServerInMaintenanceMode_script {

    replaceScript "ServerInMaintenanceMode"\
                  "POLICY_CONDITION"\
                  "JAVASCRIPT"\
                  "ServerInMaintenanceMode.js"\
                  "Checks whether the server is in maintenance mode."
}

function replace_OIDC_Claims_Script_original_script {

    replaceScript "OIDC Claims Script"\
                  "OIDC_CLAIMS"\
                  "GROOVY"\
                  "OIDC Claims Script_original.groovy"\
                  "Default global script for OIDC claims"
                  
}

function replace_OIDC_Claims_Script_lab11end_script {

    replaceScript "OIDC Claims Script"\
                  "OIDC_CLAIMS"\
                  "GROOVY"\
                  "OIDC Claims Script_lab11-end.groovy"\
                  "Default global script for OIDC claims - lab11-end"
}

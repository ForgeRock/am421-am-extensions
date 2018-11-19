#!/bin/bash
#Create ContactListREST application with policies
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/script_functions.sh

ADMIN_TOKEN=$1

[[ $ADMIN_TOKEN == "" ]] && echo "Error: Missing tokenId parameter" && exit ;

RESOURCE_TYPE_UUID=$(curl --silent\
     --request POST\
     --header "Content-Type: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=1.0, protocol=1.0" \
     --data '{
                "name": "ContactListREST",
                "description": "Defines special patterns for the ContactList applicatins and normal HTTP methods as actions",
                "patterns": [
                    "rest://profiles",
                    "rest://profiles/*",
                    "rest://activeProfiles",
                    "rest://isProfileDisabled/*",
                    "rest://owned-groups/*"
                  ],
                "actions": {
                    "DELETE": true,
                    "POST": true,
                    "PUT": true,
                    "GET": true
                 }
            }'\
     "$SERVER_URI/json/resourcetypes?_action=create" | jq -r .uuid)

echo "Resource type created: ContactListREST (uuid:$RESOURCE_TYPE_UUID)"

POLICY_SET_NAME=$(curl --silent\
     --request POST\
     --header "Content-Type: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=1.0, protocol=1.0" \
     --data '{
                "name": "ContactListREST",
                "conditions": [
                  "LEAuthLevel",
                  "Policy",
                  "Script",
                  "AuthenticateToService",
                  "SimpleTime",
                  "AMIdentityMembership",
                  "OR",
                  "IPv6",
                  "IPv4",
                  "SelectedRoleCondition",
                  "SessionProperty",
                  "AuthScheme",
                  "AuthLevel",
                  "NOT",
                  "AuthenticateToRealm",
                  "AND",
                  "ResourceEnvIP",
                  "LDAPFilter",
                  "OAuth2Scope",
                  "Session"
                ],
                "resourceTypeUuids": [
                  "'$RESOURCE_TYPE_UUID'"
                ],
                "applicationType": "iPlanetAMWebAgentService",
                "subjects": [
                  "Policy",
                  "NOT",
                  "OR",
                  "JwtClaim",
                  "AuthenticatedUsers",
                  "AND",
                  "Identity",
                  "NONE"
                ],
                "entitlementCombiner": "DenyOverride"
              }'\
     "$SERVER_URI/json/applications?_action=create"|jq -r .name)

echo "Policy set created: $POLICY_SET_NAME"

POLICY_NAME=$(curl --silent\
             --request POST\
             --header "Content-Type: application/json"\
             --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
             --header "Accept-API-Version: resource=2.0, protocol=1.0" \
             --data '{
                        "name": "ContactReaderRESTPrivileges",
                        "description": "Contact Readers can read the active profiles, all the contact-groups and all the contacts",
                        "resources": [
                           "rest://activeProfiles",
                           "rest://owned-groups/*"
                        ],
                        "applicationName": "'$POLICY_SET_NAME'",
                        "actionValues": {
                          "GET": true
                        },
                        "subject": {
                          "type":"AuthenticatedUsers"
                        },
                        "condition": {
                          "type":"SessionProperty",
                          "ignoreValueCase": false,
                          "properties": {
                            "selectedRole": [
                              "ContactReader"
                            ]
                          }
                        },
                        "resourceTypeUuid": "'$RESOURCE_TYPE_UUID'",
                        "resourceAttributes": [
                          {
                            "type": "Static",
                            "propertyName": "ContactReader",
                            "propertyValues": [
                              "true"
                            ]
                          }
                        ]
                      }'\
     "$SERVER_URI/json/policies?_action=create"|jq -r .name)

echo "Policy created: $POLICY_NAME"

POLICY_NAME=$(curl --silent\
             --request POST\
             --header "Content-Type: application/json"\
             --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
             --header "Accept-API-Version: resource=2.0, protocol=1.0" \
             --data '{
                        "name": "ContactAdminRESTPrivileges",
                        "description": "Contact Admins can read the active profiles, they have full privileges for all the contact-groups and for all the contacts.",
                        "resources": [
                           "rest://activeProfiles",
                           "rest://owned-groups/*"
                        ],
                        "applicationName": "'$POLICY_SET_NAME'",
                        "actionValues": {
                          "GET": true,
                          "POST": true,
                          "PUT": true,
                          "DELETE": true
                        },
                        "subject": {
                          "type":"AuthenticatedUsers"
                        },
                        "condition": {
                          "type":"SessionProperty",
                          "ignoreValueCase": false,
                          "properties": {
                            "selectedRole": [
                              "ContactAdmin"
                            ]
                          }
                        },
                        "resourceTypeUuid": "'$RESOURCE_TYPE_UUID'",
                        "resourceAttributes": [
                          {
                            "type": "Static",
                            "propertyName": "ContactAdmin",
                            "propertyValues": [
                              "true"
                            ]
                          }
                        ]
                      }'\
     "$SERVER_URI/json/policies?_action=create"|jq -r .name)

echo "Policy created: $POLICY_NAME"

POLICY_NAME=$(curl --silent\
             --request POST\
             --header "Content-Type: application/json"\
             --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
             --header "Accept-API-Version: resource=2.0, protocol=1.0" \
             --data '{
                        "name": "ProfileAdminRESTPrivileges",
                        "description": "Profile Admins are allowed to do any profile related actions",
                        "resources": [
                           "rest://profiles",
                           "rest://profiles/*"
                        ],
                        "applicationName": "'$POLICY_SET_NAME'",
                        "actionValues": {
                          "GET": true,
                          "POST": true,
                          "PUT": true,
                          "DELETE": true
                        },
                        "subject": {
                          "type": "AuthenticatedUsers"
                        },
                        "condition": {
                          "type": "SessionProperty",
                          "ignoreValueCase": false,
                          "properties": {
                            "selectedRole": [
                              "ProfileAdmin"
                            ]
                          }
                        },
                        "resourceTypeUuid": "'$RESOURCE_TYPE_UUID'",
                        "resourceAttributes": [
                          {
                            "type": "Static",
                            "propertyName": "ProfileAdmin",
                            "propertyValues": [
                              "true"
                            ]
                          }
                        ]
                      }'\
     "$SERVER_URI/json/policies?_action=create"|jq -r .name)

echo "Policy created: $POLICY_NAME"

findScript "ResourceIsOwnedByCurrentUser"

POLICY_NAME=$(curl --silent\
             --request POST\
             --header "Content-Type: application/json"\
             --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
             --header "Accept-API-Version: resource=2.0, protocol=1.0" \
             --data '{
                        "name": "AllContactPermissionsToResourceOwners",
                        "description": "Owners of contact groups and contacts have full privileges over their owned contact groups and contacts.",
                        "resources": [
                           "rest://owned-groups/*"
                        ],
                        "applicationName": "'$POLICY_SET_NAME'",
                        "actionValues": {
                          "GET": true,
                          "POST": true,
                          "PUT": true,
                          "DELETE": true
                        },
                        "subject": {
                          "type": "AuthenticatedUsers"
                        },
                        "condition": {
                          "type": "Script",
                          "scriptId": "'$SCRIPT_ID'"
                        },
                        "resourceTypeUuid": "'$RESOURCE_TYPE_UUID'",
                        "resourceAttributes": [
                          {
                            "type": "Static",
                            "propertyName": "ResourceOwner",
                            "propertyValues": [
                              "true"
                            ]
                          }
                        ]
                      }'\
     "$SERVER_URI/json/policies?_action=create"|jq -r .name)

echo "Policy created: $POLICY_NAME"

findScript "ServerInMaintenanceMode"

POLICY_NAME=$(curl --silent\
             --request POST\
             --header "Content-Type: application/json"\
             --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
             --header "Accept-API-Version: resource=2.0, protocol=1.0" \
             --data '{
                        "name": "DenyPostPutDeleteDuringServerMaintenance",
                        "description": "Deny create, modify and delete operations during maintenance mode",
                        "resources": [
                           "rest://owned-groups/*",
                           "rest://profiles",
                           "rest://profiles/*"
                        ],
                        "applicationName": "'$POLICY_SET_NAME'",
                        "actionValues": {
                          "POST": false,
                          "PUT": false,
                          "DELETE": false
                        },
                        "subject": {
                          "type": "AuthenticatedUsers"
                        },
                        "condition": {
                          "type": "Script",
                          "scriptId": "'$SCRIPT_ID'"
                        },
                        "resourceTypeUuid": "'$RESOURCE_TYPE_UUID'"
                      }'\
     "$SERVER_URI/json/policies?_action=create"|jq -r .name)

echo "Policy created: $POLICY_NAME"
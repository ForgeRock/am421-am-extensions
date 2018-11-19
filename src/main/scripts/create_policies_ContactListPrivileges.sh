#!/bin/bash
#Create ContactListPrivileges application with policies
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
                "name": "ContactListPrivileges",
                "description": "Defines all of the possible privileges in the ContactList applications as actions. There is only a single resource called privileges.",
                "patterns": [
                  "privileges"
                ],
                "actions": {
                  "users/delete": true,
                  "contact-groups/read": true,
                  "users/read": true,
                  "contacts/create": true,
                  "contact-groups/create": true,
                  "contacts/read": true,
                  "contacts/modify": true,
                  "contact-groups/modify": true,
                  "users/create": true,
                  "contacts/delete": true,
                  "users/modify": true,
                  "contact-groups/delete": true
                }
              }'\
     "$SERVER_URI/json/resourcetypes?_action=create" | jq -r .uuid)

echo "Resource type created: ContactListPrivileges (uuid: $RESOURCE_TYPE_UUID)"

POLICY_SET_NAME=$(curl --silent\
     --request POST\
     --header "Content-Type: application/json"\
     --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
     --header "Accept-API-Version: resource=1.0, protocol=1.0" \   
     --data '{
                "name": "ContactListPrivileges",
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
                        "name": "ContactAdminPrivileges",
                        "description": "ContactAdmins can read and write all the contactgroups and all the contacts",
                        "resources": [
                          "privileges"
                        ],
                        "applicationName": "'$POLICY_SET_NAME'",
                        "actionValues": {
                          "contact-groups/read": true,
                          "contact-groups/create": true,
                          "contact-groups/modify": true,
                          "contact-groups/delete": true,
                          "contacts/read": true,
                          "contacts/create": true,
                          "contacts/modify": true,
                          "contacts/delete": true
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
                        "name": "ContactReaderPrivileges",
                        "description": "ContactReaders can read contact groups and contacts",
                        "resources": [
                          "privileges"
                        ],
                        "applicationName": "'$POLICY_SET_NAME'",
                        "actionValues": {
                          "contact-groups/read": true,
                          "contacts/read": true
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
                        "name": "ProfileAdminPrivileges",
                        "description": "Profile Admins are allowed to do any user related actions",
                        "resources": [
                          "privileges"
                        ],
                        "applicationName": "'$POLICY_SET_NAME'",
                        "actionValues": {
                          "users/delete": true,
                          "users/create": true,
                          "users/modify": true,
                          "users/read": true
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

findScript "ServerInMaintenanceMode"

POLICY_NAME=$(curl --silent\
             --request POST\
             --header "Content-Type: application/json"\
             --header "iPlanetDirectoryPro: $ADMIN_TOKEN"\
             --header "Accept-API-Version: resource=2.0, protocol=1.0" \
             --data '{
                        "name": "DenyWritePrivilegesDuringServerMaintenance",
                        "description": "Deny create, modify and delete operations during maintenance mode",
                        "resources": [
                           "privileges"
                        ],
                        "applicationName": "'$POLICY_SET_NAME'",
                        "actionValues": {
                          "contact-groups/create": false,
                          "contact-groups/modify": false,
                          "contact-groups/delete": false,
                          "contacts/create": false,
                          "contacts/modify": false,
                          "contacts/delete": false,
                          "users/delete": false,
                          "users/create": false,
                          "users/modify": false
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

#!/bin/bash
# FR-421 - OpenAM Setup script
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh



case $1 in
    "lab01-end"|"lab02-end"|"lab03-end"|"lab04-end"|"lab05-end"|"lab06-end"|"lab07-end"|"lab08-end"|"lab09-end"|"lab10-end"|"lab11-end"|"lab12-end" )
        echo "####################"
        tomcat_stop
        cd $PROJECT_DIR;
        mvn clean install
        $SCRIPT_DIR/manage_deployments.sh $1
        $SCRIPT_DIR/init_identities.sh
        $SCRIPT_DIR/manage_scripts.sh $1
        $SCRIPT_DIR/manage_authentication.sh $1
        $SCRIPT_DIR/manage_policies.sh $1
        $SCRIPT_DIR/manage_oauth2.sh $1
        app_tomcat_stop
        cd $APP_PROJECT_DIR;
        mvn clean install
        app_tomcat_start
        ;;
    *)
      echo "Usage: $0 labXX-end, where XX can be 01..12"
      echo "~/fr_scripts.config can override configuration variables"
esac

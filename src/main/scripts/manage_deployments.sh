#!/bin/bash
# FR-421 - openam-extensions deployer script
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh

function copy_jar {
   echo "Deploying $JARFILE..."
   if [ -f $PROJECT_DIR/target/$JARFILE ]; then
      cp $PROJECT_DIR/target/$JARFILE $TOMCAT_HOME/webapps/openam/WEB-INF/lib/
      cp $PROJECT_DIR/target/$JARFILE $TOOLS_DIR/lib/
   else
      echo AuthModule jar does not exist. Please compile it. Exit.
      exit
   fi
}

function copy_callbacks_xml {
   echo "Copying $AUTHMODULE.xml..."
   cp $PROJECT_DIR/src/main/resources/$AUTHMODULE.xml $TOMCAT_HOME/webapps/openam/config/auth/default
}

function remove_jar {
   echo "Undeploying $JARFILE..."
   rm -f $TOMCAT_HOME/webapps/openam/WEB-INF/lib/$JARFILE
   rm -f $TOOLS_DIR/lib/$JARFILE
}

function remove_callbacks_xml { 
   echo "Removing $AUTHMODULE.xml..."
   rm -f $TOMCAT_HOME/webapps/openam/config/auth/default/$AUTHMODULE.xml
}

case $1 in
    "undeploy"|"lab01-end" )
        echo "####################"
        tomcat_stop
        remove_jar
	remove_callbacks_xml
        ;;
    "lab02-end"|"lab03-end" )
        echo "####################"
        tomcat_stop
	remove_callbacks_xml
        copy_jar
        ;;
    "deploy"|"lab04-end"|"lab05-end"|"lab06-end"|"lab07-end"|"lab08-end"|"lab09-end"|"lab10-end"|"lab11-end"|"lab12-end" )
        echo "####################"
        tomcat_stop
        copy_jar
	copy_callbacks_xml
        ;;
    *)
      echo "Usage: $0 deploy|undeploy|labXX-end where XX can be 01..12"
      echo "~/fr_scripts.config can override configuration variables"
esac

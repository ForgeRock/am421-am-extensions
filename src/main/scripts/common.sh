#!/bin/bash
# FR-421 - Environment setter script. Used by other scripts
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
PACKAGE=com.forgerock.edu.authmodule
AUTHMODULE=SelectRole
JARFILE=am-extensions-1.0-SNAPSHOT.jar
INSTANCE=selectRole
CHAIN=testSelectRole

TOMCAT_HOME=/opt/tomcats/login
APP_TOMCAT_HOME=/opt/tomcats/app
TOOLS_DIR=~/ssoadmintools
PROJECT_DIR=~/NetBeansProjects/openam-extensions
APP_PROJECT_DIR=~/NetBeansProjects/contactlist

SSOADM=$TOOLS_DIR/openam/bin/ssoadm
PASSFILE=$TOOLS_DIR/.password.openam
SERVICE_DEF=${AUTHMODULE}ServiceDef.xml
SERVER_URI="http://login.example.com:18080/am"

base64Encode='base64 --wrap=0'

test -f ~/fr_scripts.config && source ~/fr_scripts.config

function base64Encode {
    $base64Encode $1
}

function tomcat_start {
   PID=$(jps -v | grep $TOMCAT_HOME | cut -d " " -f 1)
   [ "$PID" != "" ] && return 
   echo Starting Tomcat
   $TOMCAT_HOME/bin/catalina.sh jpda start >/dev/null
   while read LINE; do
    if [[ $LINE =~ " Server startup in " ]]; then
        break
    fi
    done < <(tail -1f $TOMCAT_HOME/logs/catalina.out)
}

function tomcat_stop {
   PID=$(jps -v | grep $TOMCAT_HOME | cut -d " " -f 1)
   [ "$PID" = "" ] && return
   echo Stopping Tomcat
   $TOMCAT_HOME/bin/shutdown.sh >/dev/null 2>/dev/null
   while kill -0 $PID 2>/dev/null; do
       sleep 1
   done
}

function app_tomcat_start {
   PID=$(jps -v | grep $APP_TOMCAT_HOME | cut -d " " -f 1)
   [ "$PID" != "" ] && return 
   echo Starting App Tomcat
   $APP_TOMCAT_HOME/bin/catalina.sh jpda start >/dev/null
   while read LINE; do
    if [[ $LINE =~ " Server startup in " ]]; then
        break
    fi
    done < <(tail -1f $APP_TOMCAT_HOME/logs/catalina.out)
}

function app_tomcat_stop {
   PID=$(jps -v | grep $APP_TOMCAT_HOME | cut -d " " -f 1)
   [ "$PID" = "" ] && return
   echo Stopping App Tomcat
   $APP_TOMCAT_HOME/bin/shutdown.sh >/dev/null 2>/dev/null
   while kill -0 $PID 2>/dev/null; do
       sleep 1
   done
}

function obtain_admin_token {
   ADMIN_TOKEN=$($SCRIPT_DIR/authenticate.sh amadmin $(cat $PASSFILE))
   echo "Admin token obtained" 
   export ADMIN_TOKEN
}

#!/bin/bash
# FR-421 - Tomcat manager, can be used to start, stop or restart tomcats
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh

function print_usage {
      echo "Usage: $0 tomcatName command "
      echo "where tomcatName = login|app"
      echo "         command = start|stop|restart"
      echo "~/fr_scripts.config can override configuration variables"
}

case $1 in
    "login" )
        case $2 in
            "start" )
                tomcat_start
                ;;
            "stop" )
                tomcat_stop
                ;;
            "restart" )
                tomcat_stop
                tomcat_start
                ;;
            *)
                print_usage
        esac
        ;;
    "app" )
        case $2 in
            "start" )
                app_tomcat_start
                ;;
            "stop" )
                app_tomcat_stop
                ;;
            "restart" )
                app_tomcat_stop
                app_tomcat_start
                ;;
            *)
                print_usage
        esac
        ;;
    *)
    print_usage
esac


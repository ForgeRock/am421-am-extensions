#!/bin/bash
# FR-421 - OpenAM service managing script
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh
source $SCRIPT_DIR/script_functions.sh

function delete_timecheck_module_and_chain {
   echo Deleting service, module and chain
   echo "delete-auth-cfgs       -e / -m TimeCheckChain" > /tmp/ssoadm_script
   echo "delete-auth-instances  -e / -m TimeCheckModule" >> /tmp/ssoadm_script
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script | grep -v "^$"
   rm -f /tmp/ssoadm_script
}

function create_timecheck_module_and_chain {
   echo Creating TimeCheck module and chain
   rm -f /tmp/ssoadm_script /tmp/ssoadm_script_param
   cat <<END > /tmp/ssoadm_script
create-auth-instance   -e / -m TimeCheckModule -t Scripted
update-auth-instance -e / -m TimeCheckModule -D /tmp/ssoadm_script_param
create-auth-cfg      -e / -m TimeCheckChain
add-auth-cfg-entr    -e / -m TimeCheckChain -o DataStore        -c REQUISITE
add-auth-cfg-entr    -e / -m TimeCheckChain -o TimeCheckModule  -c REQUIRED
END
   #parameterfile to update-auth-instance
    echo "iplanet-am-auth-scripted-auth-level=1" > /tmp/ssoadm_script_param
    echo "iplanet-am-auth-scripted-client-script-enabled=true" >> /tmp/ssoadm_script_param

    findScript "TimeCheck Client"
    echo "iplanet-am-auth-scripted-client-script=$SCRIPT_ID" >> /tmp/ssoadm_script_param

    findScript "TimeCheck Server"
    echo "iplanet-am-auth-scripted-server-script=$SCRIPT_ID" >> /tmp/ssoadm_script_param

    $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script  | grep -v "^$"
    rm -f /tmp/ssoadm_script /tmp/ssoadm_script_param
}

function delete_lab04_module_and_chain {
   echo Deleting service, module and chain
   echo "delete-auth-cfgs       -e / -m $CHAIN" > /tmp/ssoadm_script
   echo "delete-auth-instances  -e / -m $INSTANCE" >> /tmp/ssoadm_script
   echo "unregister-auth-module --authmodule $PACKAGE.$AUTHMODULE" >> /tmp/ssoadm_script
   echo "delete-svc             --servicename sunAMAuth${AUTHMODULE}Service" >> /tmp/ssoadm_script
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script | grep -v "^$"
   rm -f /tmp/ssoadm_script
}


function create_lab04_module_and_chain {
   echo Creating service, module and chain
   rm -f /tmp/ssoadm_script /tmp/ssoadm_script_param
   cat <<END > /tmp/ssoadm_script
create-svc             --xmlfile $PROJECT_DIR/src/main/resources/$SERVICE_DEF
register-auth-module   --authmodule $PACKAGE.$AUTHMODULE
create-auth-instance   -e / -m $INSTANCE -t $AUTHMODULE
update-auth-instance -e / -m $INSTANCE -D /tmp/ssoadm_script_param
create-auth-cfg      -e / -m $CHAIN
add-auth-cfg-entr    -e / -m $CHAIN -o DataStore  -c REQUISITE
add-auth-cfg-entr    -e / -m $CHAIN -o $INSTANCE  -c REQUIRED
END
   #parameterfile to update-auth-instance
   cat <<END > /tmp/ssoadm_script_param
defaultRoleName=ContactListUser
selectableRoleNames=ContactReader
selectableRoleNames=ContactAdmin
selectableRoleNames=ProfileAdmin
END
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script  | grep -v "^$"
   rm -f /tmp/ssoadm_script /tmp/ssoadm_script_param
}

case $1 in
    "register"|"lab04-end"|"lab05-end"|"lab06-end"|"lab07-end"|"lab08-end"|"lab09-end"|"lab10-end"|"lab11-end"|"lab12-end" )
        echo "####################"
        tomcat_start
        obtain_admin_token
        delete_timecheck_module_and_chain
	delete_lab04_module_and_chain
        create_timecheck_module_and_chain
	create_lab04_module_and_chain
        ;;
    "unregister"|"lab01-end"|"lab02-end"|"lab03-end")
        echo "####################"
	tomcat_start
        obtain_admin_token
        delete_timecheck_module_and_chain
        delete_lab04_module_and_chain
        create_timecheck_module_and_chain
        ;;
    *)
      echo "Usage: $0 register|unregister|labXX-end     where XX can be 01..12"
      echo "~/fr_scripts.config can override configuration variables"
esac


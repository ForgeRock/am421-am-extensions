#!/bin/bash
# FR-421 - OpenAM service managing script
SCRIPT_DIR="$( dirname "$( which "$0" )" )"
source $SCRIPT_DIR/common.sh

function copy_files {
   echo Copying files
   if [ -f $PROJECT_DIR/target/$JARFILE ]; then
      cp $PROJECT_DIR/target/$JARFILE $TOMCAT_HOME/webapps/openam/WEB-INF/lib/
      cp $PROJECT_DIR/src/main/resources/$AUTHMODULE.xml $TOMCAT_HOME/webapps/openam/config/auth/default
      cp $PROJECT_DIR/target/$JARFILE $TOOLS_DIR/lib/
   else
      echo AuthModule jar does not exist. Please compile it. Exit.
      exit
   fi
}

function create_module_and_chain {
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

function create_users_and_groups {
   echo Creating groups
   echo "create-identity   --idname ContactReader --idtype Group --realm /" > /tmp/ssoadm_script
   echo "create-identity   --idname ContactAdmin --idtype Group --realm /" >> /tmp/ssoadm_script
   echo "create-identity   --idname ProfileAdmin --idtype Group --realm /" >> /tmp/ssoadm_script
   echo "create-identity   --idname SessionPropertyManager --idtype Group --realm /" >> /tmp/ssoadm_script
   echo "add-privileges    --idname SessionPropertyManager \
                           --privileges SessionPropertyModifyAccess --idtype Group --realm /" >> /tmp/ssoadm_script
   echo "create-identity   --idname contactreader --idtype User --realm /" >> /tmp/ssoadm_script
   echo "create-identity   --idname contactadmin --idtype User --realm /" >> /tmp/ssoadm_script
   echo "create-identity   --idname profileadmin --idtype User --realm /" >> /tmp/ssoadm_script
   echo "create-identity   --idname superadmin --idtype User --realm /" >> /tmp/ssoadm_script
   echo "create-identity   --idname contactListBackend --idtype User --realm /" >> /tmp/ssoadm_script
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script | grep -v "^$"
   rm -f /tmp/ssoadm_script
}


function delete_module_and_chain {
   echo Deleting service, module and chain
   echo "delete-auth-cfgs       -e / -m $CHAIN" > /tmp/ssoadm_script
   echo "delete-auth-instances  -e / -m $INSTANCE" >> /tmp/ssoadm_script
   echo "unregister-auth-module --authmodule $PACKAGE.$AUTHMODULE" >> /tmp/ssoadm_script
   echo "delete-svc             --servicename sunAMAuth${AUTHMODULE}Service" >> /tmp/ssoadm_script
   $SSOADM do-batch -u amadmin -f $PASSFILE -c -Z /tmp/ssoadm_script | grep -v "^$"
   rm -f /tmp/ssoadm_script
}

function remove_files {
   echo Removing files
   rm -f $TOMCAT_HOME/webapps/openam/WEB-INF/lib/$JARFILE
   rm -f $TOMCAT_HOME/webapps/openam/config/auth/default/$AUTHMODULE.xml
   rm -f $TOOLS_DIR/lib/$JARFILE
}

case $1 in
    "deploy" )
        echo "####################"
        tomcat_stop
	copy_files
        ;;
    "register" )
        echo "####################"
	copy_files
        tomcat_start
	delete_module_and_chain
	create_module_and_chain
        tomcat_stop
        ;;
    "unregister" )
        echo "####################"
	tomcat_start
        delete_module_and_chain
    	tomcat_stop
        ;;
    *)
      echo "Usage: $0 deploy|register|unregister"
      echo "~/fr_scripts.config can override configuration variables"
esac

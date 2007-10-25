#!/bin/sh

# -----------------------------------------------------------------------------
#  This script should be used to start the Spring Container from within FUSE Services Framework.
#
# Environment Variable Prequisites
#   CXF_HOME       Must Point to the FUSE Services Framework install directory
#   JAVA_HOME      Must Point to your JDk installation directory
# -----------------------------------------------------------------------------

# ensure all the needed environment variables are set
if [ "${CXF_HOME:-x}" = "x" ]
then
  echo "CXF_HOME must point to the FUSE Services Framework install directory."
  echo "Please set CXF_HOME and run this script again."
  return 1
fi
if [ "${JAVA_HOME:-x}" = "x" ]
then
  echo "JAVA_HOME must point to your JDK installation."
  echo "Please set JAVA_HOME and run this script again."
  return 1
  fi
fi

echo "setting SPRING_CONTAINER_HOME to "$CXF_HOME"/containers/spring_container"
SPRING_CONTAINER_HOME="$CXF_HOME"/containers/spring_container
export SPRING_CONTAINER_HOME

LOGGING_PROPS=$CXF_HOME/etc/logging.properties

SPRING_CONTAINER_CLASSPATH=$SPRING_CONTAINER_HOME/etc:$CLASSPATH:$CXF_HOME/lib/it-soa-container.jar:$CXF_HOME/lib/cxf-manifest-incubator.jar:$ACTIVEMQ_HOME/activemq-all-$ACTIVEMQ_VERSION.jar:$CLASSPATH

$JAVA_HOME/bin/java -cp $SPRING_CONTAINER_CLASSPATH -Djava.util.logging.config.file=$LOGGING_PROPS com.iona.cxf.container.ContainerLauncher "$@"


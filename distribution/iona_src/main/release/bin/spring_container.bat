@echo off
if "%OS%" == "Windows_NT" setlocal
rem ----------------------------------------------------------------------------
rem This script should be used to start the Spring Container from within FUSE Services Framework.
rem
rem Environment Variable Prequisites
rem   CXF_HOME      Must Point to the FUSE Services Framework install directory
rem   JAVA_HOME        Must Point to your JDk installation directory
rem ----------------------------------------------------------------------------

rem Parse CXF_HOME to create TMP value that does not contain quotes.
for /F "delims=" %%i in ("%CXF_HOME%") do set CXF_HOME_TMP=%%~i
    
rem Parse JAVA_HOME to create TMP value that does not contain quotes.
for /F "delims=" %%j in ("%JAVA_HOME%") do set JAVA_HOME_TMP=%%~j
    
rem ensure all the needed environment variables are set
if not "%CXF_HOME_TMP%" == "" goto gotCXFHome
echo CXF_HOME must point to the FUSE Services Framework install directory.
echo Please set CXF_HOME and run this script again.
goto exitWithError

:gotCXFHome
if not "%JAVA_HOME_TMP%" == "" (
 rem Strip quotes
 set JAVA_HOME=%JAVA_HOME:"=%
) 

if not "%JAVA_HOME%" == "" goto springHomeSet
echo 
echo JAVA_HOME must point to your JDK installation
echo Please set JAVA_HOME and run this script again.
goto exitWithError

:springHomeSet

echo Setting SPRING_CONTAINER_HOME to %CXF_HOME%\containers\spring_container
set SPRING_CONTAINER_HOME=%CXF_HOME%\containers\spring_container

set LOGGING_PROPS=%CXF_HOME%\etc\logging.properties

set SPRING_CONTAINER_CLASSPATH=%SPRING_CONTAINER_HOME%/etc;%CXF_HOME%\lib\it-soa-container.jar;%CXF_HOME%\lib\cxf-manifest-incubator.jar;%ACTIVEMQ_HOME%\activemq-all-%ACTIVEMQ_VERSION%.jar;%CLASSPATH%

"%JAVA_HOME%\bin\java" -cp "%SPRING_CONTAINER_CLASSPATH%" -Djava.util.logging.config.file=%LOGGING_PROPS% com.iona.cxf.container.ContainerLauncher  %*
goto end

:exitWithError
echo start_container ended with error
echo Spring Container has not been started successfully
exit /b 1

:end
exit /b 0


Container Hello World Demo
==========================

The Spring Container is a lightweight container for deploying service endpoints. 
This demo shows how to create service artifacts for an endpoint and provides an 
example spring.xml file required to deploy service endpoints as Spring beans in
the Spring Container. These elements are wrapped in a WAR file that can be 
deployed to the repository directory defined by the Spring Container. 

Once you have deployed the WAR file, you can start the Spring Container and run 
a generated client against the endpoint deployed in the container.

Prerequisites
-------------

CXF_HOME should be set to the FUSE Services Framework installation directory. 

Building and running the demo using ant
---------------------------------------

The ant build.xml script in this directory is used to build and run this demo.

Using either UNIX or Windows:

1. Run "ant create.war" to create a hello_world.war file in the demo build 
   directory. 

2. Run "ant copy.war" to copy hello_world.war from the build directory to 
   <install_dir>/containers/spring_container/repository.
  
3. Run "<install_dir>/bin/spring_container(.bat) start" to start the Spring 
   Container and run the deployed application.
  
4. Run "ant client" to run the client against the endpoint deployed in the 
   Spring container
    
5. Run "<install_dir>/bin/jmx_console_start(.bat) to launch the Java JMX 
   Console. This allows you to view the "SpringContainer" MBean deployed as 
   part of the container. The MBean gives you access to the management
   interface for the Spring Container. You can use it to list, stop, start and 
   deploy applications.

/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container.managed;

import com.iona.cxf.container.ApplicationState;
import com.iona.cxf.container.ContainerException;

/**
 * Interface which defines JMX MBean methods used to manage the Spring Container
 */

public interface JMXContainer {

    /**
     * Used to deploy a new application to the container repository. This method copies a war file from 
     * a given URL or File location into the container repository using the specified name for the copied war.
     * If deployment fails, the copied file and its corresponding exploded directory will be deleted.
     * @param location a URL or File location pointing to the application to be deployed.
     * @param warFileName the name of the copied file as it will appear in the container repository, i.e.
     * using the name "test" will cause a file named "test.war" to appear in the container repository
     * @throws ContainerException  if deployment fails
     */    
    void deploy(String location, String warFileName) throws ContainerException;

    /**
     * Stops the specified application based upon its name. This does not remove the application
     * from the container repository.
     * @param name 
     * @throws ContainerException if stopping the application fails.
     */
    void stopApplication(String name) throws ContainerException;

    /**
     * Starts an application that has previously been deployed and subsequently stopped. 
     * @param name the name of the application to start
     * @throws ContainerException if starting the applicatoin fails.
     */
    void startApplication(String name) throws ContainerException;

    /**
     * Stops and removes an application. This action completely removes an application from the
     * container repository.
     * @param name the name of the application to remove from the container repository.
     * @throws ContainerException if stopping the application fails or if removing artifacts
     * from the container repository fails.
     */
    void removeApplication(String name) throws ContainerException;

    /**
     * Lists all applications that have been deployed. These applications may be in either a start 
     * or stop state.
     * @returns list of application names.
     */
    String[] listApplicationNames();

    ApplicationState getApplicationState(String name) throws ContainerException;

}


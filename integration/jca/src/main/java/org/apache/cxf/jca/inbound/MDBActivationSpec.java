/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jca.inbound;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

/**
 * MDBActivationSpec is for activating a CXF service endpoint facade.  
 */
public class MDBActivationSpec implements ActivationSpec {

    private ResourceAdapter resouceAdapter;
    private String wsdlURL;
    private String serviceInterfaceClass;
    private String busConfigurationURL;
    private String address;
    private String endpointName;
    
    /**
     * Gets the transport address used by 
     * {@link org.apache.cxf.frontend.ServerFactoryBean}.
     * 
     * @return the address
     */
    public String getAddress() {
        return address;
    }


    /**
     * Gets the CXF bus configuration URL.  
     * 
     * The resource location should be within the Message Driven Bean jar.
     * 
     * @return the busConfigurationURL
     */
    public String getBusConfigurationURL() {
        return busConfigurationURL;
    }


    /**
     * A unique name that is readable to human and it is to
     * identify an inbound endpoint within a application service.
     *  
     * @return the endpointName
     */
    public String getEndpointName() {
        return endpointName;
    }


    public ResourceAdapter getResourceAdapter() {
        return resouceAdapter;
    }

    /**
     * Gets the service endpoint interface classname.  
     * 
     * The class should be available in the Message Driven Bean jar.
     * 
     * @return the serviceInterfaceClass
     */
    public String getServiceInterfaceClass() {
        return serviceInterfaceClass;
    }


    /**
     * The resource location should be within the Message Driven Bean jar.
     * 
     * @return the wsdlURL
     */
    public String getWsdlURL() {
        return wsdlURL;
    }


    /**
     * Sets the transport address used by 
     * {@link org.apache.cxf.frontend.ServerFactoryBean}.
     * 
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }


    /**
     * The class should be available in the Message Driven Bean jar.
     * 
     * @param busConfigurationURL the busConfigurationURL to set
     */
    public void setBusConfigurationURL(String busConfigurationURL) {
        this.busConfigurationURL = busConfigurationURL;
    }


    /**
     * @param endpointName the endpointName to set
     */
    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        resouceAdapter = ra;
    }


    /**
     * @param serviceInterfaceClass the serviceInterfaceClass to set
     */
    public void setServiceInterfaceClass(String serviceInterfaceClass) {
        this.serviceInterfaceClass = serviceInterfaceClass;
    }


    /**
     * The resource location should be within the Message Driven Bean jar.
     * 
     * @param wsdlURL the wsdlURL to set
     */
    public void setWsdlURL(String wsdlURL) {
        this.wsdlURL = wsdlURL;
    }


    /**
     * TODO implement validation
     */
    public void validate() throws InvalidPropertyException {
    }

}

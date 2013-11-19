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

package org.apache.cxf.endpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@ManagedResource(componentName = "Endpoint", 
                 description = "Responsible for managing server instances.")

public class ManagedEndpoint implements ManagedComponent, ServerLifeCycleListener {
    public static final String ENDPOINT_NAME = "managed.endpoint.name";
    public static final String SERVICE_NAME = "managed.service.name";
    public static final String INDENTION = "    ";
    private static final Logger LOG = LogUtils.getL7dLogger(ManagedEndpoint.class);
    
    private final String eol = System.getProperty("line.separator");

    private Bus bus;
    private Endpoint endpoint;
    private Server server;
    private enum State { CREATED, STARTED, STOPPED };
    private State state = State.CREATED;
    
    private ConfigurationAdmin configurationAdmin;
    
    public ManagedEndpoint(Bus b, Endpoint ep, Server s) {
        bus = b;
        endpoint = ep;
        server = s;
    }

    @ManagedOperation        
    public void start() {
        if (state == State.STARTED) {
            return;
        }
        ServerLifeCycleManager mgr = bus.getExtension(ServerLifeCycleManager.class);
        if (mgr != null) {
            mgr.registerListener(this);
        }
        server.start();
    }
    
    @ManagedOperation
    public void stop() {
        server.stop();
    }
    
    @ManagedOperation
    public void destroy() {
        server.destroy();
    }

    @ManagedAttribute(description = "Address Attribute", currencyTimeLimit = 60)
    public String getAddress() {
        return endpoint.getEndpointInfo().getAddress();
    }
    
    @ManagedAttribute(description = "TransportId Attribute", currencyTimeLimit = 60)
    public String getTransportId() {
        return endpoint.getEndpointInfo().getTransportId();
    }
    
    @ManagedAttribute(description = "Server State")
    public String getState() {
        return state.toString();
    }
    
    @ManagedAttribute(description = "The cxf servlet context", currencyTimeLimit = 60)
    public String getServletContext() {
        if (!isInOSGi()) {
            LOG.log(Level.FINE, "Not In OSGi.");
            return null; //not in OSGi container
        }
        String ret = "/cxf"; //if can't get it from configAdmin use the default value
        if (getConfigurationAdmin() != null) {
            try {
                Configuration configuration = getConfigurationAdmin().getConfiguration("org.apache.cxf.osgi");
                if (configuration != null) {
                    Dictionary properties = configuration.getProperties();
                    if (properties != null) {
                        String servletContext = (String)configuration.getProperties().
                            get("org.apache.cxf.servlet.context");
                        if (servletContext != null) {
                            ret = servletContext;
                        }
                    }
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "getServletContext failed.", e);
            }
        }
        return ret;
    }
    
    @ManagedAttribute(description = "if the endpoint has swagger doc or not", currencyTimeLimit = 60)
    public boolean isSwagger() {
        if (!isWADL()) {
            return false;
        }
        List<Feature> features = server.getEndpoint().getActiveFeatures();
        if (features != null) {
            for (Feature feature : features) {
                if (feature.getClass().getName().endsWith("SwaggerFeature")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @ManagedAttribute(description = "if the endpoint has wsdl doc or not", currencyTimeLimit = 60)
    public boolean isWSDL() {
        return !isWADL();
    }
    
    @ManagedAttribute(description = "if the endpoint has WADL doc or not", currencyTimeLimit = 60)
    public boolean isWADL() {
        if (endpoint.getEndpointInfo().getBinding().
            getBindingId().equals("http://apache.org/cxf/binding/jaxrs")) {
            return true;
        }
        return false;
    }
    
    @ManagedOperation(description = "get the JSON schema from a given endpoint", currencyTimeLimit = 60)
    public String getJSONSchema() {
        String ret = "";
        if (!isWSDL()) {
            Set<Class<?>> resourceTypes = (Set<Class<?>>)endpoint.get("jaxrs.resource.types");
            if (resourceTypes != null) {
                try {
                    ret = ret + getBeginIndentionWithReturn(1) + "\""
                        + "definitions" + "\" " + " : {"
                        + getEol();
                    for (Class<?> cls : resourceTypes) {
                        ret = ret + getIndention(2) + "\"" + cls.getName() + "\" : "
                            + getBeginIndentionWithReturn(0);
                        
                        ret = ret
                            + reformatIndent(JsonSchemaLookup.getSingleton()
                                                 .getSchemaForClass(cls), 3);
                        ret = ret + getEndIndentionWithReturn(2) + getEol();
                    }
                    ret = ret + getEndIndentionWithReturn(1) + getEol();
                    ret = ret + getEndIndentionWithReturn(0) + getEol();
                } catch (Throwable e) {
                    LOG.log(Level.WARNING, "getJSONSchema failed.", e);
                }
            }
        } else {

            for (ServiceInfo serviceInfo : endpoint.getService().getServiceInfos()) {
                for (BindingInfo bindingInfo : serviceInfo.getBindings()) {
                    for (BindingOperationInfo boi : bindingInfo.getOperations()) {
                        ret = ret + getBeginIndentionWithReturn(1) + "\"operations\" : "
                              + getBeginIndentionWithReturn(2) + "\""
                              + boi.getOperationInfo().getName().getLocalPart() + "\" " + " : "
                              + getBeginIndentionWithReturn(3);
                        if (boi.getInput() != null && boi.getInput().getMessageParts() != null) {
                            ret = ret + "\"input\" : " + getBeginIndentionWithReturn(4) + "\"type\" : \""
                                  + boi.getOperationInfo().getInputName() + "\""
                                  + getEndIndentionWithReturn(3) + getEol();

                        }
                        if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null) {
                            ret = ret + getIndention(3) + "\"output\" : " + getBeginIndentionWithReturn(4)
                                  + "\"type\" : \"" + boi.getOperationInfo().getOutputName() + "\""
                                  + getEndIndentionWithReturn(3) + getEol();
                        }
                        ret = ret + getEndIndentionWithReturn(2);
                    }
                    if (ret.length() > 0) {
                        ret = ret + getEndIndentionWithReturn(1);
                    }
                    Set<String> addedType = new HashSet<String>();
                    for (BindingOperationInfo boi : bindingInfo.getOperations()) {
                        ret = ret + getEol() + getIndention(1) + "\"definitions\" : "
                              + getBeginIndentionWithReturn(2);
                        if (boi.getInput() != null && boi.getInput().getMessageParts() != null
                            && !addedType.contains(boi.getOperationInfo().getInputName())) {

                            ret = ret + "\"" + boi.getOperationInfo().getInputName() + "\" : "
                                  + getBeginIndentionWithReturn(0);
                            for (MessagePartInfo mpi : boi.getInput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null) {
                                    ret = ret
                                          + reformatIndent(JsonSchemaLookup.getSingleton()
                                                               .getSchemaForClass(partClass), 3);
                                }
                            }
                            ret = ret + getEndIndentionWithReturn(2) + getEol();
                            addedType.add(boi.getOperationInfo().getInputName());

                        }
                        if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null
                            && !addedType.contains(boi.getOperationInfo().getOutputName())) {

                            ret = ret + getIndention(2) + "\"" + boi.getOperationInfo().getOutputName()
                                  + "\" : " + getBeginIndentionWithReturn(0);

                            for (MessagePartInfo mpi : boi.getOutput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null) {
                                    ret = ret
                                          + reformatIndent(JsonSchemaLookup.getSingleton()
                                                               .getSchemaForClass(partClass), 3);
                                }
                            }
                            ret = ret + getEndIndentionWithReturn(2);
                            addedType.add(boi.getOperationInfo().getOutputName());

                        }
                    }
                    if (ret.length() > 0) {
                        ret = ret + getEndIndentionWithReturn(1);
                    }

                    if (ret.length() > 0) {
                        ret = ret + getEndIndentionWithReturn(0);
                    }
                }
            }
            
        }
        return ret;
    }
    
    @ManagedOperation(description = "get the JSON schema from a given soap endpoint for a given operation", 
                        currencyTimeLimit = 60)
    public String getJSONSchemaForOperation(String operationName) {
        if (!isWSDL()) {
            return null;
        }
        String ret = "";
        
        for (ServiceInfo serviceInfo : endpoint.getService().getServiceInfos()) {
            for (BindingInfo bindingInfo : serviceInfo.getBindings()) {
                for (BindingOperationInfo boi : bindingInfo.getOperations()) {
                    if (operationName.equals(boi.getOperationInfo().getName().getLocalPart())) {
                        ret = ret + getBeginIndentionWithReturn(1) + "\""
                              + boi.getOperationInfo().getName().getLocalPart() + "\" " + " : "
                              + getBeginIndentionWithReturn(2);
                        if (boi.getInput() != null && boi.getInput().getMessageParts() != null) {
                            ret = ret + "\"input\" : " + getBeginIndentionWithReturn(4) + "\"type\" : \""
                                  + boi.getOperationInfo().getInputName() + "\""
                                  + getEndIndentionWithReturn(2) + getEol();

                        }
                        if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null) {
                            ret = ret + getIndention(2) + "\"output\" : " + getBeginIndentionWithReturn(4)
                                  + "\"type\" : \"" + boi.getOperationInfo().getOutputName() + "\""
                                  + getEndIndentionWithReturn(2) + getEol();
                        }
                        ret = ret + getEndIndentionWithReturn(1);
                        
                        ret = ret + getEol() + getIndention(1) + "\"definitions\" : "
                              + getBeginIndentionWithReturn(2);
                        if (boi.getInput() != null && boi.getInput().getMessageParts() != null) {
                            ret = ret + "\"" + boi.getOperationInfo().getInputName() + "\" : "
                                  + getBeginIndentionWithReturn(0);
                            for (MessagePartInfo mpi : boi.getInput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null) {
                                    ret = ret
                                          + reformatIndent(JsonSchemaLookup.getSingleton()
                                                               .getSchemaForClass(partClass), 3);
                                }
                            }
                            ret = ret + getEndIndentionWithReturn(2) + getEol();
                        }
                        if (boi.getOutput() != null && boi.getOutput().getMessageParts() != null) {
                            ret = ret + getIndention(2) + "\"" + boi.getOperationInfo().getOutputName()
                                  + "\" : " + getBeginIndentionWithReturn(0);

                            for (MessagePartInfo mpi : boi.getOutput().getMessageParts()) {
                                Class<?> partClass = mpi.getTypeClass();
                                if (partClass != null) {
                                    ret = ret
                                          + reformatIndent(JsonSchemaLookup.getSingleton()
                                                               .getSchemaForClass(partClass), 3);
                                }
                            }
                            ret = ret + getEndIndentionWithReturn(2);
                        }
                        
                    }
                    if (ret.length() > 0) {
                        ret = ret + getEndIndentionWithReturn(1);
                    }
                    
                    if (ret.length() > 0) {
                        ret = ret + getEndIndentionWithReturn(0);
                    }
                }
            }
        }
        return ret;
    }
    
    private String reformatIndent(String input, int startIndent) {
        String ret = "";
        BufferedReader reader = new BufferedReader(new StringReader(input));
        try {
            String oneLine;
            while ((oneLine = reader.readLine()) != null) {
                ret = ret + getIndention(startIndent) + oneLine + getEol();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "reformatIndent failed.", e);
        }
        return ret;
    }
    
    private boolean isInOSGi() {
        if (FrameworkUtil.getBundle(ManagedEndpoint.class) != null) {
            return true;
        }
        return false;
        
    }
    
    
    private String getBeginIndentionWithReturn(int n) {
        return "{" + getEol() + getIndention(n);           
    }
    
    private String getEndIndentionWithReturn(int n) {
        return getEol() + getIndention(n) + "}";           
    }
    
    private String getIndention(int n) {
        String ret = "";
        for (int i = 0; i < n; i++) {
            ret = ret + INDENTION;
        }
        return ret;     
    }
    
    private String getEol() {
        if (eol == null) {
            return "\n";
        } else {
            return this.eol;
        }
    }
    
    private ConfigurationAdmin getConfigurationAdmin() {
        try {
            if (isInOSGi() && (configurationAdmin == null)) {
                BundleContext bundleContext = FrameworkUtil.getBundle(ManagedEndpoint.class)
                    .getBundleContext();
                if (bundleContext != null) {
                    ServiceReference serviceReference = bundleContext
                        .getServiceReference(ConfigurationAdmin.class.getName());
                    if (serviceReference != null) {
                        configurationAdmin = (ConfigurationAdmin)bundleContext.getService(serviceReference);
                    }
                }

            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "getConfigurationAdmin failed.", e);
        }
        return configurationAdmin;
    }
        
    public ObjectName getObjectName() throws JMException {
        String busId = bus.getId();
        StringBuilder buffer = new StringBuilder();
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':');
        buffer.append(ManagementConstants.BUS_ID_PROP).append('=').append(busId).append(',');
        buffer.append(ManagementConstants.TYPE_PROP).append('=').append("Bus.Service.Endpoint,");
       

        String serviceName = (String)endpoint.get(SERVICE_NAME);
        if (StringUtils.isEmpty(serviceName)) {
            serviceName = endpoint.getService().getName().toString();
        }
        serviceName = ObjectName.quote(serviceName);
        buffer.append(ManagementConstants.SERVICE_NAME_PROP).append('=').append(serviceName).append(',');
        
        
        String endpointName = (String)endpoint.get(ENDPOINT_NAME);
        if (StringUtils.isEmpty(endpointName)) {
            endpointName = endpoint.getEndpointInfo().getName().getLocalPart();
        }
        endpointName = ObjectName.quote(endpointName);
        buffer.append(ManagementConstants.PORT_NAME_PROP).append('=').append(endpointName).append(',');
        // Added the instance id to make the ObjectName unique
        buffer.append(ManagementConstants.INSTANCE_ID_PROP).append('=').append(endpoint.hashCode());
        
        //Use default domain name of server
        return new ObjectName(buffer.toString());
    }

    public void startServer(Server s) {
        if (server.equals(s)) {
            state = State.STARTED;            
        }
    }

    public void stopServer(Server s) {
        if (server.equals(s)) {
            state = State.STOPPED;
            // unregister server to avoid the memory leak
            ServerLifeCycleManager mgr = bus.getExtension(ServerLifeCycleManager.class);
            if (mgr != null) {
                mgr.unRegisterListener(this);                
            }
        }
    }
}

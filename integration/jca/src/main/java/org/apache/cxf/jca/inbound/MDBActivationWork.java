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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.EndpointUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

/**
 *
 * MDBActivationWork is a type of {@link Work} that to be executed by 
 * {@link javax.resource.spi.work.WorkManager}.  MDBActivationWork
 * starts an CXF service endpoint which accepts inbound calls for
 * the JCA connector.
 * 
 */
public class MDBActivationWork implements Work {
    
    private static final Logger LOG = LogUtils.getL7dLogger(MDBActivationWork.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_SLEEP = 5000;

    private MDBActivationSpec spec;
    private MessageEndpointFactory endpointFactory;
    private boolean released;

    private Server server;
    private Map<String, InboundEndpoint> endpoints;

    public MDBActivationWork(MDBActivationSpec spec, 
            MessageEndpointFactory endpointFactory, 
            Map<String, InboundEndpoint> endpoints) {
        this.spec = spec;
        this.endpointFactory = endpointFactory;
        this.endpoints = endpoints;
    }

    public void release() {
        released = true;
    }

    /**
     * Performs the work
     */
    public void run() {
        // create service endpoint interface class
        if (spec.getServiceInterfaceClass() != null) {
            
            // get the classloader from the event driven bean
            MessageEndpoint endpoint = getMesssageEndpoint();
            if (endpoint != null) {
                try {
                    ClassLoader classLoader = endpoint.getClass().getClassLoader();
                    Class<?> clazz = Class.forName(spec.getServiceInterfaceClass(),
                            false, classLoader); 
                    
                    // create server bean factory
                    ServerFactoryBean factory = EndpointUtils.hasWebServiceAnnotation(clazz) 
                        ? new JaxWsServerFactoryBean() : new ServerFactoryBean();
                    factory.setServiceClass(clazz);
                    factory.setAddress(spec.getAddress());
                    
                    MDBInvoker invoker = createInvoker(endpoint);
                    factory.setInvoker(invoker);
                    
                    // create and start the server
                    server = factory.create();
                    server.start();
                    
                    // save the server for clean up later
                    endpoints.put(spec.getEndpointName(), new InboundEndpoint(server, invoker));
                    
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to activate service endpoint " 
                            + spec.getEndpointName(), e);
                }
            }
        }
    }
    
    /**
     * @param endpoint
     * @return
     */
    private MDBInvoker createInvoker(MessageEndpoint endpoint) {
        MDBInvoker answer = null;
        if (spec instanceof DispatchMDBActivationSpec) {
            answer = new DispatchMDBInvoker(endpoint, 
                    ((DispatchMDBActivationSpec)spec).getTargetBeanJndiName());
        } else {
            answer = new MDBInvoker(endpoint);
        }
        return answer;
    }

    /**
     * Invokes endpoint factory to create message endpoint (event driven bean).
     * It will retry if the event driven bean is not yet available.
     */
    private MessageEndpoint getMesssageEndpoint() {
        MessageEndpoint answer = null;
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            
            if (released) {
                LOG.warning("CXF service activation has been stopped.");
                return null;
            }
            
            try {
                answer = endpointFactory.createEndpoint(null);
                break;
            } catch (UnavailableException e) {
                LOG.fine("Target endpoint activation in progress.  Will retry.");
                try {
                    Thread.sleep(RETRY_SLEEP);
                } catch (InterruptedException e1) {
                    // ignore
                }
            }
        }
        
        if (answer == null) {
            LOG.severe("Failed to activate  service endpoint " 
                    + spec.getEndpointName() 
                    + " due to unable to endpoint listener.");
        }
        
        return answer;
    }
}

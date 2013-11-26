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

package org.apache.cxf.systest.jaxws;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.ws.Endpoint;
    
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.testutil.common.TestUtil;

import org.apache.hello_world.GreeterImpl;


import org.junit.Assert;
import org.junit.Test;

public class JsonSchemaJMXTest extends Assert {
    
    static final String PORT = TestUtil.getPortNumber(JsonSchemaJMXTest.class);
    private static MBeanServerConnection mbsc;
    private static final String DEFAULT_JMXSERVICE_URL = 
        "service:jmx:rmi:///jndi/rmi://localhost:9914/jmxrmi";
    private static final Logger LOG = LogUtils.getL7dLogger(JsonSchemaJMXTest.class);
    
    
    private String jmxServerURL;
   
    @Test
    public void testJMXGetJsonSchema() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf
                .createBus("/org/apache/cxf/systest/jaxws/jmx-enable.xml");
        BusFactory.setDefaultBus(bus);
        Endpoint ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort",
                                       new GreeterImpl());
        try {
            connectToMBserver();
            String json = invokeEndpoint("getJSONSchema");
            System.out.println("the json is \n" + json);
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "FAIL_TO_INVOKE_OPERATION", new Object[]{e});
        } finally {
            ep.stop();
        }
    }
    
    void connectToMBserver() throws IOException {
        jmxServerURL = jmxServerURL == null ? DEFAULT_JMXSERVICE_URL : jmxServerURL; 
        JMXServiceURL url = new JMXServiceURL(jmxServerURL);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        mbsc = jmxc.getMBeanServerConnection();
    }
    
    ObjectName getEndpointObjectName() 
        throws MalformedObjectNameException, NullPointerException {
        StringBuilder buffer = new StringBuilder();
        String serviceName = "{http://apache.org/hello_world/services}SOAPService";
        String portName = "SoapPort";
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":type=Bus.Service.Endpoint,");
        buffer.append(ManagementConstants.SERVICE_NAME_PROP + "=\"" + serviceName + "\",");
        buffer.append(ManagementConstants.PORT_NAME_PROP + "=\"" + portName + "\",*");        
        return new ObjectName(buffer.toString());
    }
    
    private String invokeEndpoint(String operation) {
        ObjectName endpointName = null;
        ObjectName queryEndpointName;
        String ret = "";
        try {
            queryEndpointName = getEndpointObjectName();
            Set<ObjectName> endpointNames = CastUtils.cast(mbsc.queryNames(queryEndpointName, null));
            // now get the ObjectName with the busId
            Iterator<ObjectName> it = endpointNames.iterator();
        
            if (it.hasNext()) {
                // only deal with the first endpoint object which retrun from the list.
                endpointName = it.next();
                ret = (String)mbsc.invoke(endpointName, operation, new Object[0], new String[0]);
                System.out.println("invoke endpoint " + endpointName 
                                   + " operation " + operation + " succeed!");
            }
            
        } catch (Exception e) {
            if (null == endpointName) {
                LOG.log(Level.SEVERE, "FAIL_TO_CREATE_ENDPOINT_OBEJCTNAME", new Object[]{e});
                
            } else {
                LOG.log(Level.SEVERE, "FAIL_TO_INVOKE_MANAGED_OBJECT_OPERATION",
                    new Object[]{endpointName, operation, e.toString()});
            }
        } 
        return ret;
    }

}

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

import java.util.Map;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.junit.BeforeClass;
import org.junit.Test;

public class AttachmentsTest extends AbstractBusClientServerTestBase {
  
    static final Logger LOG = LogUtils.getLogger(AttachmentsTest.class);

    private final QName portName = 
        new QName("http://apache.org/hello_world_soap_http", "SoapPort");

    @BeforeClass
    public static void startServers() throws Exception {                    
        assertTrue("server did not launch correctly",
                   launchServer(MtomEnabledServer.class));
    }

    @Test
    public void testAttachments() throws Exception {
        SOAPService service = new SOAPService();
        assertNotNull(service);

        Greeter greeter = service.getPort(portName, Greeter.class);
        BindingProvider bp = (BindingProvider)greeter;
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        binding.setMTOMEnabled(true);

        greeter.greetMe("add attachments");

        Map<String, Object> responseContext = bp.getResponseContext();
        Map dataHandlers = (Map)
            responseContext.get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS);
        assertNotNull("expected data handlers", dataHandlers);
        assertEquals(3, dataHandlers.size());
        assertNotNull(dataHandlers.get("foo"));
        assertEquals("unexpected content", 
                     "FOO", 
                     ((DataHandler)dataHandlers.get("foo")).getContent());
        assertNotNull(dataHandlers.get("bar"));
        assertEquals("unexpected content", 
                     "BAR",
                     ((DataHandler)dataHandlers.get("bar")).getContent());
        assertNotNull(dataHandlers.get("snafu"));
        assertEquals("unexpected content", 
                     "SNAFU",
                     ((DataHandler)dataHandlers.get("snafu")).getContent());
    }    
}

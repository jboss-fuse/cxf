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

package org.apache.cxf.ws.security.policy.interceptors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.policy.custom.NTLMTokenBuilder;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxInInterceptor;



public class NTLMTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {

    private static final long serialVersionUID = 8824623117854629741L;
    
    
    public NTLMTokenInterceptorProvider() {
        super(Arrays.asList(NTLMTokenBuilder.NTLM_AUTHENTICATION));
        this.getOutInterceptors().add(new NTLMTokenOutInterceptor());
        this.getOutFaultInterceptors().add(new NTLMTokenOutInterceptor());
        this.getInInterceptors().add(new NTLMTokenInInterceptor());
        this.getInFaultInterceptors().add(new NTLMTokenInInterceptor());
    }
    
    private static Map<String, List<String>> getProtocolHeaders(Message message) {
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));        
        if (null == headers) {
            return Collections.emptyMap();
        }
        return headers;
    }

    static class NTLMTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
        NTLMTokenOutInterceptor() {
            super(Phase.PRE_STREAM);
        }
        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                
                Collection<AssertionInfo> ais = 
                    aim.get(NTLMTokenBuilder.NTLM_AUTHENTICATION);
                if (ais.isEmpty()) {
                    return;
                }
                if (isRequestor(message)) {
                    assertNTLM(aim, ais, message);
                } else {
                    //server side should be checked on the way in
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
            }
        }
        private void assertNTLM(AssertionInfoMap aim, Collection<AssertionInfo> ais, Message message) {
            Map<String, List<String>> headers = getProtocolHeaders(message);
            for (AssertionInfo ai : ais) {
                List<String> auth = headers.get("Authorization");
                if (auth == null || auth.size() == 0 
                    || !auth.get(0).startsWith("Basic")) {
                    ai.setNotAsserted("NtlmAuthentication is set, but not being used");
                } else {
                    ((HTTPConduit)message.getExchange().
                        getConduit(message)).getClient().setAllowChunking(false);
                    ai.setAsserted(true);
                }
                if (!ai.isAsserted()) {
                    throw new PolicyException(ai);
                }
            }                 
        }
    }
    
    static class NTLMTokenInInterceptor extends AbstractPhaseInterceptor<Message> {
        NTLMTokenInInterceptor() {
            super(Phase.PRE_STREAM);
            addBefore(WSS4JStaxInInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = 
                    aim.get(NTLMTokenBuilder.NTLM_AUTHENTICATION);
                boolean requestor = isRequestor(message);
                if (ais.isEmpty() && !requestor) {
                    return;
                }
                if (!requestor) {
                    //server side can simply ignore this 
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                    
                } else {
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                    
                }
            }
        }
    }
}

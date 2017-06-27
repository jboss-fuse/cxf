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

package org.apache.cxf.ws.security.policy.custom;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.neethi.Policy;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.SPConstants.SPVersion;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.AbstractToken;


public class NTLMToken extends AbstractToken {
    
     
    public NTLMToken(SPVersion version, IncludeTokenType includeTokenType, Element issuer,
                        String issuerName, Element claims, Policy nestedPolicy) {
        super(version, includeTokenType, issuer, issuerName, claims, nestedPolicy);
    }

    @Override
    public QName getName() {
        return NTLMTokenBuilder.NTLM_AUTHENTICATION;
    }

    @Override
    protected AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy) {
        return new NTLMToken(getVersion(), 
                             getIncludeTokenType(), getIssuer(), getIssuerName(), getClaims(), nestedPolicy);
    }
    

}

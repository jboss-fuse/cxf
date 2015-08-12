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

package org.apache.cxf.jaxrs.swagger;

import com.wordnik.swagger.jaxrs.config.BeanConfig;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

public class SetScannerInterceptor extends AbstractPhaseInterceptor {

    private BeanConfigWrapper beanConfigWrapper;
    
    public SetScannerInterceptor(String phase, BeanConfigWrapper beanConfigWrapper) {
        super(phase);
        this.beanConfigWrapper = beanConfigWrapper;
    }

    @Override
    public void handleMessage(Message m) throws Fault {
        MessageContextImpl mci = new MessageContextImpl(m);
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(beanConfigWrapper.getResourcePackage());
        beanConfig.setVersion(beanConfigWrapper.getVersion());
        beanConfig.setBasePath(beanConfigWrapper.getBasePath());
        beanConfig.setTitle(beanConfigWrapper.getTitle());
        beanConfig.setDescription(beanConfigWrapper.getDescription());
        beanConfig.setContact(beanConfigWrapper.getContact());
        beanConfig.setLicense(beanConfigWrapper.getLicense());
        beanConfig.setLicenseUrl(beanConfigWrapper.getLicenseUrl());
        beanConfig.setScan(beanConfigWrapper.isScan());
        if (mci.getServletContext() != null) {
            mci.getServletContext().setAttribute("SCANNER", beanConfig);
        }
    }

}

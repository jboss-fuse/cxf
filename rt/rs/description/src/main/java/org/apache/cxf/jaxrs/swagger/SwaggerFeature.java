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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.jaxrs.config.BeanConfig;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.phase.Phase;

public class SwaggerFeature extends AbstractSwaggerFeature {

    @Override
    protected void addSwaggerResource(Server server) {
        ApiListingResourceJSON apiListingResource = new ApiListingResourceJSON();
        if (!runAsFilter) {
            List<Object> serviceBeans = new ArrayList<Object>();
            serviceBeans.add(apiListingResource);
            ((JAXRSServiceFactoryBean)server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName())).
                setResourceClassesFromBeans(serviceBeans);
        }
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ResourceListingProvider());
        providers.add(new ApiDeclarationProvider());
        
        BeanConfigWrapper beanConfigWrapper = new BeanConfigWrapper();
        beanConfigWrapper.setResourcePackage(getResourcePackage());
        beanConfigWrapper.setVersion(getVersion());
        beanConfigWrapper.setBasePath(getBasePath());
        beanConfigWrapper.setTitle(getTitle());
        beanConfigWrapper.setDescription(getDescription());
        beanConfigWrapper.setContact(getContact());
        beanConfigWrapper.setLicense(getLicense());
        beanConfigWrapper.setLicenseUrl(getLicenseUrl());
        beanConfigWrapper.setTermsOfServiceUrl(getTermsOfServiceUrl());
        beanConfigWrapper.setScan(isScan());
        beanConfigWrapper.setFilterClass(getFilterClass());
        server.getEndpoint().getInInterceptors().add(new SetScannerInterceptor(Phase.PRE_INVOKE, beanConfigWrapper));
        if (runAsFilter) {
            providers.add(new SwaggerContainerRequestFilter(apiListingResource, beanConfigWrapper));
        }
        ((ServerProviderFactory)server.getEndpoint().get(ServerProviderFactory.class.getName()))
            .setUserProviders(providers);
    }    

    @Override
    protected void setBasePathByAddress(String address) {
        setBasePath(address);
    }

    @PreMatching
    private static class SwaggerContainerRequestFilter implements ContainerRequestFilter {
        private static final String APIDOCS_LISTING_PATH = "api-docs";
        private static final Pattern APIDOCS_RESOURCE_PATH = Pattern.compile(APIDOCS_LISTING_PATH + "(/.+)");
        
        private ApiListingResourceJSON apiListingResource;
        private BeanConfigWrapper beanConfigWrapper;
        @Context
        private MessageContext mc;
        SwaggerContainerRequestFilter(ApiListingResourceJSON apiListingResource, BeanConfigWrapper beanConfigWrapper) {
            this.apiListingResource = apiListingResource;
            this.beanConfigWrapper = beanConfigWrapper;
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            UriInfo ui = mc.getUriInfo();
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
            beanConfig.setTermsOfServiceUrl(beanConfigWrapper.getTermsOfServiceUrl());
            beanConfig.setFilterClass(beanConfigWrapper.getFilterClass());
            mc.getServletContext().setAttribute("SCANNER", beanConfig);
            ServletConfig sc = mc.getServletConfig();
            if (sc == null) {
                sc = new ServletConfig() {
                    @Override
                    public String getServletName() {
                        return null;
                    }

                    @Override
                    public ServletContext getServletContext() {
                        return mc.getServletContext();
                    }

                    @Override
                    public String getInitParameter(String name) {
                        return null;
                    }

                    @Override
                    public Enumeration<String> getInitParameterNames() {
                        return null;
                    }
                };
            }
            if (ui.getPath().endsWith(APIDOCS_LISTING_PATH)) {
                Response r = 
                    apiListingResource.resourceListing(null, sc, mc.getHttpHeaders(), ui);
                requestContext.abortWith(r);
            } else {
                final Matcher matcher = APIDOCS_RESOURCE_PATH.matcher(ui.getPath());
                
                if (matcher.find()) {
                    Response r = 
                        apiListingResource.apiDeclaration(matcher.group(1), 
                            null, mc.getServletConfig(), mc.getHttpHeaders(), ui);
                    requestContext.abortWith(r);                
                }
            }
        }
        
    }
}

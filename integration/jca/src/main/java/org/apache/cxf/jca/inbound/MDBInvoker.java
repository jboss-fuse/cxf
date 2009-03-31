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

import java.util.logging.Logger;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.AbstractTwoStageCache;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.AbstractInvoker;

/**
 * Invoker that supports direct Message Driven Bean invocation.
 * Applications that put service implementation inside the Message 
 * Driven Bean that activates the inbound endpoint facade should
 * choose this invoker.  It is more straightforward and faster than 
 * {@link DispatchMBDInvoker} but it requires to modify Resource
 * Adapter's deployment descriptor.
 */
public class MDBInvoker extends AbstractInvoker {
    private static final Logger LOG = LogUtils.getL7dLogger(MDBInvoker.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_SLEEP = 5000;
    private static final int CACHE_SIZE = 10;
    
    private final MessageEndpointFactory endpointFactory;
    private final AbstractTwoStageCache<MessageEndpoint> cache;

    /**
     * @param messageEndpoint
     */
    public MDBInvoker(MessageEndpointFactory factory, int cacheSize) throws Exception {
        endpointFactory = factory;
        if (cacheSize == 0) {
            cacheSize = CACHE_SIZE;
        }
        cache = new AbstractTwoStageCache<MessageEndpoint>(cacheSize, cacheSize, 0) {
            public MessageEndpoint create() throws Exception {
                MessageEndpoint mep = createMessageEndpoint();
                if (mep == null) {
                    throw new Exception("Failed to create MessageEndpoint.");                  
                }
                
                return mep;
            }

            /**
             * Invokes endpoint factory to create message endpoint (event driven bean).
             * It will retry if the event driven bean is not yet available.
             */
            private MessageEndpoint createMessageEndpoint() {
                MessageEndpoint ep = null;
                for (int i = 0; i < MAX_ATTEMPTS; i++) {
                    try {
                        ep = endpointFactory.createEndpoint(null);
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
                
                return ep;
            }
        };
        
        cache.populateCache();
    }

    public void release() {
        MessageEndpoint mep = cache.poll();
        while (mep != null) {
            mep.release();
            mep = cache.poll();
        }
    }

    /**
     * @return the messageEndpoint
     */
    public MessageEndpoint getMessageEndpoint() {
        try {
            return cache.get();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void recycleEndpoint(MessageEndpoint mep) {
        boolean wasRecycled = cache.recycle(mep);
        if (!wasRecycled) {
            mep.release();
        }
    }

    @Override
    public Object getServiceObject(Exchange context) {
        return getMessageEndpoint();
    }

}

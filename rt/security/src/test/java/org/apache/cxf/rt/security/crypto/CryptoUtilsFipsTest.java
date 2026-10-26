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
package org.apache.cxf.rt.security.crypto;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.security.Security;

import org.apache.cxf.helpers.JavaUtils;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

public class CryptoUtilsFipsTest {

    @Test
    public void testFipsInitializationUsesConfiguredProviders() throws Exception {
        Provider[] providers = Security.getProviders();
        URL[] locations = {
            CryptoUtils.class.getProtectionDomain().getCodeSource().getLocation(),
            JavaUtils.class.getProtectionDomain().getCodeSource().getLocation()
        };
        // Isolate the static FIPS state and CryptoUtils initialization from other tests.
        try (ProviderTrackingClassLoader loader = new ProviderTrackingClassLoader(locations)) {
            Class<?> javaUtils = loader.loadClass(JavaUtils.class.getName());
            Field enabled = javaUtils.getDeclaredField("isFIPSEnabled");
            enabled.setAccessible(true);
            enabled.setBoolean(null, true);
            Field provider = javaUtils.getDeclaredField("fipsSecurityProvider");
            provider.setAccessible(true);
            provider.set(null, null);

            Class.forName(CryptoUtils.class.getName(), true, loader);

            assertFalse("CXF must not load a cryptographic provider in FIPS mode", loader.providerRequested);
            assertArrayEquals("CXF must preserve the configured security providers",
                              providers, Security.getProviders());
        }
    }

    private static final class ProviderTrackingClassLoader extends URLClassLoader {
        private boolean providerRequested;

        ProviderTrackingClassLoader(URL[] locations) {
            super(locations, CryptoUtilsFipsTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if ("org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider".equals(name)) {
                providerRequested = true;
                throw new ClassNotFoundException(name);
            }
            if (CryptoUtils.class.getName().equals(name) || JavaUtils.class.getName().equals(name)) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = findClass(name);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}

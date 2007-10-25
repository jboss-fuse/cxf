/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC
 *                       All Rights Reserved.
 */

package com.iona.cxf.container;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import com.iona.cxf.container.classloader.ApplicationClassLoader;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;

public class Application {
    private static final Logger LOG = LogUtils.getL7dLogger(Application.class);
    private static final String SPRING_CONFIG_DIR = "META-INF/spring";
    private static final String XML_FILE_SUFFIX = ".xml";
    private Bus bus;
    private File appDir;
    private String appName;
    private ClassLoader loader;
    private ApplicationState state;

    public Application(String name, File dir) throws ContainerException {
        appName = name;
        appDir = dir;
        state = ApplicationState.STOPPED;
    }

    public File getApplicationDirectory() {
        return appDir;
    }
    
    public String getName() {
        return appName;
    }
    
    public Set<QName> getServices() {
        
        Set<QName> services = new HashSet<QName>();
        
        if (bus != null) {
            ServerRegistry registry = bus.getExtension(ServerRegistry.class);
            if (registry != null) {
                for (Server server : registry.getServers()) {
                    services.add(server.getEndpoint().getService().getName());
                }
            }
        }
        
        return services;
    }

    public synchronized void start() throws ContainerException {
        changeState(Event.START);
    }

    public synchronized void stop() throws ContainerException {
        changeState(Event.STOP);
    }

    public ApplicationState getState() {
        return state;
    }

    private void changeState(Event event) throws ContainerException {
        switch (event) {
        case STOP:
            if (state == ApplicationState.STARTED) {
                try {
                    cleanup();
                } finally {
                    state = ApplicationState.STOPPED;
                }
            }
            break;

        case START:
            if (state == ApplicationState.STOPPED || state == ApplicationState.FAILED) {                
                try {
                    init();
                    state = ApplicationState.STARTED;
                } catch (ContainerException cex) {
                    state = ApplicationState.FAILED;
                    cleanup();
                    throw cex;
                }
            }
            
            break;
            
        default:

        }
    }

    private void init() throws ContainerException {       
        ClassLoader orig = Thread.currentThread().getContextClassLoader();            

        try {
            loader = ApplicationClassLoader.createClassLoader(appDir);            
            Thread.currentThread().setContextClassLoader(loader);
            
            SpringBusFactory bf = new SpringBusFactory();
            
            File springConfDir = new File(appDir, SPRING_CONFIG_DIR);
            
            File[] files = springConfDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(XML_FILE_SUFFIX);
                }
            });
            

            if (null != files) {
                if (files.length > 1) {
                    throw new ContainerException(new Message("TOO_MANY_SPRING_BEAN_DEFINITIONS", LOG)); 
                } else if (files.length < 1) {
                    throw new ContainerException(new Message("MISSING_SPRING_BEAN_DEFINITIONS", LOG));
                }

                String res = "spring/" + files[0].getName();
                bus = bf.createBus(res);
                
            } else {
                throw new ContainerException(new Message("MISSING_SPRING_BEAN_DEFINITIONS", LOG));
            }
            
        } catch (Exception ex) {
            throw new ContainerException(ex);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

    private void cleanup() {
        ClassLoader orig = Thread.currentThread().getContextClassLoader();            

        try {
            Thread.currentThread().setContextClassLoader(loader);
            bus.shutdown(true);
        } finally {
            bus = null;
            loader = null;
            Thread.currentThread().setContextClassLoader(orig);
        }
    }
    
    enum Event { START, STOP }
}

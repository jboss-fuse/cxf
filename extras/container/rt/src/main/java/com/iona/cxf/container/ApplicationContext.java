/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;

import com.iona.cxf.container.classloader.ApplicationClassLoader;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

class ApplicationContext {
    private static final Logger LOG = LogUtils.getL7dLogger(ApplicationContext.class);
    private static final String SPRING_CONFIG_DIR = "META-INF/spring";
    private static final String XML_FILE_SUFFIX = ".xml";
    private GenericApplicationContext context;
    private File appDir;
    private ClassLoader loader;
    private ApplicationState state;

    public ApplicationContext(File dir) throws ContainerException {
        appDir = dir;
        state = ApplicationState.STOPPED;
    }

    public File getApplicationDirectory() {
        return appDir;
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

            context = new GenericApplicationContext();
            XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
            reader.setBeanClassLoader(loader);
            
            File springConfDir = new File(appDir, SPRING_CONFIG_DIR);
            
            File[] files = springConfDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(XML_FILE_SUFFIX);
                }
            });

            if (null != files) {
                for (File file : files) {
                    Resource resource = new FileSystemResource(file);
                    reader.loadBeanDefinitions(resource);                
                }
            } else {
                throw new ContainerException(new Message("MISSING_SPRING_BEAN_DEFINITIONS", LOG));
            }
            
            context.refresh();
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
            context.close();
        } finally {
            context = null;
            loader = null;
            Thread.currentThread().setContextClassLoader(orig);
        }
    }
    
    enum Event { START, STOP }
}

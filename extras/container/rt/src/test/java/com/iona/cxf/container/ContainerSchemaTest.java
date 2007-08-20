/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container;

import java.net.URL;

//import org.junit.After;
import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.UrlResource;

public class ContainerSchemaTest extends Assert {

    @Test
    public void testNamespaceHandler() throws Exception { 
        URL url = ContainerSchemaTest.class.getResource("/container_test.xml");      
        GenericApplicationContext context = createContext(url);
        ConfigurableListableBeanFactory factory = context.getBeanFactory();
        try {
            factory.getBean("container1");
            fail("Incorrect repositoy location should cause Exception.");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().indexOf("x1/repository") >= 0);
        }
    }

    private GenericApplicationContext createContext(URL url) {
        GenericApplicationContext context = new GenericApplicationContext();
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);        
        reader.setBeanClassLoader(getClass().getClassLoader());
        reader.setNamespaceAware(true);
        reader.loadBeanDefinitions(new UrlResource(url));

        return context;
    }
    
}

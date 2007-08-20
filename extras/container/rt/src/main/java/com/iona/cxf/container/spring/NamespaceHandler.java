package com.iona.cxf.container.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class NamespaceHandler extends NamespaceHandlerSupport {
    public void init() {
        registerBeanDefinitionParser("container", new ContainerBeanDefinitionParser());        
    }
}

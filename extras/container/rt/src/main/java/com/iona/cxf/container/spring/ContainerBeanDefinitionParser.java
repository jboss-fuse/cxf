package com.iona.cxf.container.spring;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.iona.cxf.container.ContainerBean;

import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;


public class ContainerBeanDefinitionParser extends AbstractBeanDefinitionParser {

    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        NamedNodeMap atts = element.getAttributes();
        for (int i = 0; i < atts.getLength(); i++) {
            Attr node = (Attr) atts.item(i);
            
            mapToProperty(bean, node.getLocalName(), node.getValue());
        }
    }

    @Override
    protected Class getBeanClass(Element arg0) {
        return ContainerBean.class;
    }

}

/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container.classloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

public class FirewallClassLoader extends ClassLoader {
    private static final Logger LOG = LogUtils.getL7dLogger(FirewallClassLoader.class);
    private final Map filterTree;

    public FirewallClassLoader(ClassLoader parent, Map filters) {
        super(parent);
        filterTree = filters;
    }
  
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        List<String> tokens = tokenizeName(name, ".");
        Map map = filterTree;

        for (int i = 0; i < tokens.size() - 1; ++i) {
            map = (Map)map.get(tokens.get(i));
            if (map == null) {
                break;
            } 
        }

        if (map == null || map.get(tokens.get(tokens.size() - 1) + ".class") == null) {
            return super.loadClass(name, resolve);
        } else {
            LOG.fine("FirewallClassloader blocking lookup of Class: " + name);
            throw new ClassNotFoundException(name);
        }
    }         
    
    
    public java.net.URL getResource(String name) {
        List<String> tokens = tokenizeName(name, "/");
        Map map = filterTree;
        
        for (int i = 0; i < tokens.size() - 1; ++i) {
            map = (Map)map.get(tokens.get(i));
            if (map == null) {
                break;
            } 
        }
        
        if (map == null || map.get(tokens.get(tokens.size() - 1)) == null) {
            return super.getResource(name);
        } else {
            LOG.fine("FirewallClassloader blocking lookup of Resource: " + name);
            return null;
        }
    }  
    
    private List<String> tokenizeName(String name, String delimiters) {
        List<String> list = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(name, delimiters);
        
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        
        return list;
    }

}

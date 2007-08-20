/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container.classloader;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import com.iona.cxf.container.ContainerException;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;

public final class ApplicationClassLoader {
    private static final Logger LOG = LogUtils.getL7dLogger(ApplicationClassLoader.class);
    private static final String LIB_DIR = "WEB-INF/lib";
    private static final String CLASSES_DIR = "WEB-INF/classes";
    private static final String WSDL_DIR = "WEB-INF/wsdl";
    private static final String META_INF_DIR = "META-INF";
    private static final String[] ILLEGAL_PACKAGE_NAMES = {"java", "com/sun", "sun", "org/omg", "javax"};
    private static final String JAR_EXT = ".jar";

    private ApplicationClassLoader() {

    }

    public static ClassLoader createClassLoader(File appDir) throws ContainerException {        
        ClassLoader parent = (new ApplicationClassLoader()).getClass().getClassLoader();

        return createClassLoader(appDir, parent);
    }

    public static ClassLoader createClassLoader(File appDir, 
                                                ClassLoader realParent) throws ContainerException {
        Map<String, Object> resourceTree = new HashMap<String, Object>();
        ClassLoader loader = null;

        try {
            List<URL> urls = new ArrayList<URL>();
            File classDir = new File(appDir, CLASSES_DIR);
            
            if (classDir.exists()) {
                urls.add(classDir.toURL());
                addResourcesFromDir(classDir, "", resourceTree);
            } 

            File metaDir = new File(appDir, META_INF_DIR);
            
            if (metaDir.exists()) {
                urls.add(metaDir.toURL());
                addResourcesFromDir(metaDir, "", resourceTree);
            }             
            
            File wsdlDir = new File(appDir, WSDL_DIR);
            
            if (wsdlDir.exists()) {
                urls.add(wsdlDir.toURL());
                addResourcesFromDir(wsdlDir, "", resourceTree);
            } 

            File[] jars = getJarFiles(appDir);
            
            for (File jar : jars) {
                urls.add(jar.toURL()); 
            }
                        
            for (File file : jars) {
                if (file.isDirectory()) {
                    addResourcesFromDir(file, "", resourceTree);
                } else {
                    JarFile jar = new JarFile(file);
                    addResoucesFromJar(jar, resourceTree);
                }
            }
            
            inspectResourceTreeForIllegalPackages(resourceTree);
            
            ClassLoader parent = new FirewallClassLoader(realParent, resourceTree);
            
            for (URL url  : urls) {
                LOG.fine("Adding URL to ClassLoader; " + url);
            }

            loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
        } catch (java.net.MalformedURLException muex) {
            throw new ContainerException(muex);
        } catch (java.io.IOException ioex) {
            throw new ContainerException(ioex);
        } 
            
        return loader;
    }

    private static File[] getJarFiles(File appDir) {
        File libDir = new File(appDir, LIB_DIR);

        if (libDir.exists() && libDir.isDirectory()) {            
            File[] jars = libDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(JAR_EXT);
                    }
                });
            
            
            if (jars == null) {
                return new File[0];
            }
            
            return jars;
        }

        return new File[0];
    }

    
    private static void addResourcesFromDir(File dir, String relativePath,
                                            Map<String, Object> map) {
        File[] files = dir.listFiles();

        for (File file : files) {
            String path = relativePath + "/" + file.getName();
                
            if (file.isDirectory()) {
                addResourcesFromDir(file, path, map);
            } else {
                List<String> list = tokenizeResource(path);
                insertResourceIntoMap(map, list);                
            }
        }
    }

    private static void addResoucesFromJar(JarFile jar, Map<String, Object> resources) {
        Enumeration<JarEntry> entries = jar.entries();
        
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                String name = entry.getName();
                List<String> list = tokenizeResource(name);
                insertResourceIntoMap(resources, list); 
            }
        }
    }
    
    private static void insertResourceIntoMap(Map<String, Object> map, List<String> list) {
        if (list.size() > 1) {
            String dir = list.get(0);
            Object obj = map.get(dir);

            Map<String, Object> submap = CastUtils.cast((Map<?, ?>)obj);

            if (submap == null) {
                submap = new HashMap<String, Object>();
                map.put(dir, submap);
            }

            List<String> sublist = list.subList(1, list.size());
            insertResourceIntoMap(submap, sublist);
        } else {
            String resourceName = list.get(0);
            map.put(resourceName, resourceName);
        }
    }

    private static List<String> tokenizeResource(String resourceName) {
        List<String> list = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(resourceName, "/");
        
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        
        return list;
    }

    private static void inspectResourceTreeForIllegalPackages(Map<String, Object> tree)
        throws ContainerException {
        
        Map<String, Object> map = tree;

        for (String packageName : ILLEGAL_PACKAGE_NAMES) {
            List<String> list = tokenizeResource(packageName);
            
            for (String name : list) {
                Object obj = map.get(name);
                if (obj == null) {
                    return;
                } else {
                    map = CastUtils.cast((Map<?, ?>)obj);
                }
            }

            if (map != null) {
                throw new ContainerException(new Message("ILLEGAL_PACKAGE", LOG, 
                                                         new Object[] {packageName}));
            }
        }
    }
    
}

package com.iona.cxf.container.classloader;

import java.io.File;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import com.iona.cxf.container.util.ApplicationExploder;

import org.apache.cxf.helpers.CastUtils;


public class ClassLoaderTest extends TestCase {
    private File tmpdir;
    private File repository;
    
    public void setUp() throws Exception {
        tmpdir = new File(System.getProperty("java.io.tmpdir"));
        repository = new File(tmpdir, "repository_" + System.currentTimeMillis());
        repository.mkdir();
        repository.deleteOnExit();
    }

    public void tearDown() throws Exception {

    }

    public void testApplicationClassLoader() throws Exception {
        URL url = getClass().getResource("/test.war");
        assertNotNull(url);
        File war = new File(url.toURI());
        File warDir = ApplicationExploder.explodeApplication(war, repository);  
        File dupWar = new File(repository, "test2");
        warDir.renameTo(dupWar);
        assertTrue(dupWar.exists());
        warDir = ApplicationExploder.explodeApplication(war, repository);  
        ClassLoader loader1 = ApplicationClassLoader.createClassLoader(warDir);
        
        ClassLoader parent1 = loader1.getParent();
        assertTrue(parent1 instanceof FirewallClassLoader);
        try {
            Class.forName("com.iona.cxf.test.greeter.Greeter", true, parent1);
            fail("ClassNotFoundException should have been thrown by FirewallClassLoader.");
        } catch (ClassNotFoundException cnfex) {
            //Expected
        }

        Class clazz = Class.forName("com.iona.cxf.test.greeter.Greeter", true, loader1);
        assertNotNull(clazz);

        URL wsdlUrl = loader1.getResource("greeter.wsdl");
        assertNotNull(wsdlUrl);

        URL springUrl = loader1.getResource("spring/spring.xml");
        assertNotNull(springUrl);

        //We know loader1 can load GreeterClass so let's see if FWCL for loader2 does not find class
        //
        ClassLoader loader2 = ApplicationClassLoader.createClassLoader(dupWar, loader1);
        ClassLoader parent2 = loader2.getParent();
        assertTrue(parent2 instanceof FirewallClassLoader);
        try {
            Class.forName("com.iona.cxf.test.greeter.Greeter", true, parent2);
            fail("ClassNotFoundException should have been thrown by FirewallClassLoader.");
        } catch (ClassNotFoundException cnfex) {
            //Expected
        }

        clazz = Class.forName("com.iona.cxf.test.greeter.Greeter", true, loader2);
        assertNotNull(clazz);

        wsdlUrl = parent2.getResource("greeter.wsdl");
        assertNull(wsdlUrl);

        wsdlUrl = loader2.getResource("greeter.wsdl");
        assertNotNull(wsdlUrl);  

        boolean wasDeleted = ApplicationExploder.deleteFile(repository);
        assertTrue(wasDeleted);                                            
    }

    public void testFirewallClassLoader() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        String resource = "java/lang/String.class";
        List<String> list = tokenizeResource(resource);
        insertResourceIntoMap(map, list);
        map.put("test.war", "test.war");
        ClassLoader parent = getClass().getClassLoader();

        String className = "java.lang.String";
        Class.forName(className, true, parent);
        FirewallClassLoader fwcl = new FirewallClassLoader(parent, map);
        
        try {
            Class.forName(className, true, fwcl);
            fail("FirewallClassLoader should block request.");
        } catch (ClassNotFoundException cnfex) {
            //
        }

        URL url = parent.getResource(resource);
        assertNotNull(url);
        url = fwcl.getResource(resource);
        assertNull(url);

        boolean wasDeleted = ApplicationExploder.deleteFile(repository);
        assertTrue(wasDeleted);
    }

    private void insertResourceIntoMap(Map<String, Object> map, List<String> list) {
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

    private List<String> tokenizeResource(String resourceName) {
        List<String> list = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(resourceName, "/");
        
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        
        return list;
    }


}

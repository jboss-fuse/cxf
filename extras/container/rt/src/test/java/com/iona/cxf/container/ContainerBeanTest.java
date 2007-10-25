/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.iona.cxf.container.util.ApplicationExploder;

import org.apache.cxf.helpers.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ContainerBeanTest extends Assert {
    
    private ContainerBean containerBean;
    private File tmpdir;
    private File repository;
    
    @Before
    public void setUp() throws Exception {
        containerBean = new ContainerBean();
        tmpdir = new File(System.getProperty("java.io.tmpdir"));
        repository = new File(tmpdir, "repository_" + System.currentTimeMillis());
        repository.mkdir();
        containerBean.setContainerRepository(repository);   
    }

    @After
    public void tearDown() throws Exception {
        containerBean.shutdown();
        ApplicationExploder.deleteFile(repository);
    }
    
    @Test
    public void testAutoDeploy() throws Exception {
        copyToRepository("/test.war", "test.war");
        containerBean.run();
        String [] appNames = containerBean.listApplicationNames();
        assertEquals(1, appNames.length);
        assertEquals("test", appNames[0]);

        String [] appServices = containerBean.listApplicationServices(appNames[0]);
        assertEquals(1, appServices.length);
        assertEquals("{http://cxf.iona.com/test/greeter}GreeterService", 
                     appServices[0].toString());
        
        List<Application> applications = containerBean.getApplications();
        assertEquals(1, applications.size());
        Set<QName> services = applications.get(0).getServices();
        assertEquals(1, services.size());
        assertEquals("{http://cxf.iona.com/test/greeter}GreeterService", 
                     services.iterator().next().toString());
    }

    @Test
    public void testBadWar() throws Exception {
        copyToRepository("/test.war", "test.war");         
        File war = new File(repository, "test.war");
        RandomAccessFile file = new RandomAccessFile(war, "rw");
        FileChannel channel = file.getChannel();
        long size = channel.size();
        channel.position(size / 2);
        ByteBuffer buffer = ByteBuffer.wrap("foobar".getBytes());
        channel.write(buffer);
        channel.close();
        
        containerBean.run();
        File invalidWar = new File(repository, "test.war.corrupted");
        assertTrue(invalidWar.exists());        
    }

    @Ignore
    @Test
    public void testRestart() throws Exception {
        containerBean.run();
        copyToRepository("/test.war", "test.war");
        File location = new File(repository, "test.war");
        containerBean.deploy(location.toURL(), "my_app");
        String [] appNames = containerBean.listApplicationNames();
        assertTrue(appNames.length == 1);
        assertTrue(appNames[0].equals("TestApp"));
        containerBean.shutdown();
        assertTrue(!new File(repository, "test").exists());
        containerBean.run();
        assertTrue(appNames.length == 1);
        assertTrue(appNames[0].equals("TestApp"));
        
        /*
         * TODO: Uncomment me when TAN-151 is fixed
         * assertTrue(!new File(repository, "test").exists());
         */
    }
    
    /**
     * This test case is for testing the for TAN-148
     * @throws Exception
     */

    @Test
    public void testBadDeployUrl() throws Exception {
        containerBean.run();
        try {
            containerBean.deploy(repository.toURL() + "/my_bad", "MyApp");
            fail("URL does not point to valid WAR.");
        } catch (ContainerException ctex) {
            // Expected exception
        }
    }

    private void copyToRepository(String resource, String destination) throws Exception {
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = getClass().getResourceAsStream(resource);
            assertNotNull(is);
            os = new FileOutputStream(new File(repository, destination));
            IOUtils.copy(is, os);
        } finally {
            if (is != null) {
                is.close();
            } 
            if (os != null) {
                os.flush();
                os.close();
            }
        }
    }
    
}

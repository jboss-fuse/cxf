package com.iona.cxf.container.util;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {

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

    public void testApplicationExploder() throws Exception {
        URL url = getClass().getResource("/test.war");
        assertNotNull(url);
        File war = new File(url.toURI());
        File warDir = ApplicationExploder.explodeApplication(war, repository);        
        File wsdl = new File(warDir, "WEB-INF/wsdl/greeter.wsdl");
        assertTrue(wsdl.exists());

        boolean fileDeleted = ApplicationExploder.deleteFile(warDir);
        assertTrue(fileDeleted);
        assertTrue(!warDir.exists());

        warDir = ApplicationExploder.explodeApplication(war, repository);        
        long timestamp1 = warDir.lastModified();
        boolean wasSet = warDir.setLastModified(timestamp1 - 1000000);
        assertTrue(wasSet);
        long timestamp2 = warDir.lastModified();        
        assertTrue(timestamp1 - timestamp2 == 1000000);
        warDir = ApplicationExploder.explodeApplication(war, repository);
        long timestamp3 = warDir.lastModified();        
        assertTrue("Timestamp " + timestamp3 + " not equal to " + timestamp1,
                   Math.abs(timestamp3 - timestamp1) < 10000);

        boolean wasDeleted = ApplicationExploder.deleteFile(repository);
        assertTrue(wasDeleted);
    }

}

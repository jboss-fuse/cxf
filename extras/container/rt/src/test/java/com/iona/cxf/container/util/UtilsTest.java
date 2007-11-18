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

        long warTimestamp = war.lastModified();
        
        File warDir = ApplicationExploder.explodeApplication(war, repository);        
        File wsdl = new File(warDir, "WEB-INF/wsdl/greeter.wsdl");
        assertTrue(wsdl.exists());

        boolean wasDeleted = ApplicationExploder.deleteFile(warDir);
        assertTrue(wasDeleted);
        assertTrue(!warDir.exists());

        warDir = ApplicationExploder.explodeApplication(war, repository);        
        long timestamp1 = warDir.lastModified();
        
        warDir = ApplicationExploder.explodeApplication(war, repository);
        long timestamp2 = warDir.lastModified();  
      
        assertEquals("Exploded application has been overwritten by yonger war",
                     timestamp1, timestamp2);

        assertTrue("Exploded application's timestamp must be bigger than that of the war",
                    timestamp2 > warTimestamp);

        warDir.setLastModified(warTimestamp - 1000000);
        assertTrue("Exploded application's timestamp must now be less than that of the war",
                    warDir.lastModified() < warTimestamp);         

        warDir = ApplicationExploder.explodeApplication(war, repository);
        assertTrue("Exploded application's timestamp must've been overwritten be an older war",
                    warDir.lastModified() > warTimestamp);

        wasDeleted = ApplicationExploder.deleteFile(repository);
        assertTrue(wasDeleted);
        assertTrue(!warDir.exists());
    }

}

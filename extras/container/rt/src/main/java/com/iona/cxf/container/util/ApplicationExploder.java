/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class ApplicationExploder {

    private ApplicationExploder() {              
    }

    public static File explodeApplication(File app, File repository) throws IOException {
        String dirName = app.getName();
        dirName = dirName.substring(0, dirName.indexOf(".war"));
        File appDir = new File(repository, dirName);

        if (appDir.exists()) {
            if (app.lastModified() > appDir.lastModified()) {
                boolean wasDeleted = deleteFile(appDir);
                if (!wasDeleted) {
                    throw new IOException("Could not explode application." 
                                          + " Existing directory could not be deleted: " 
                                          + appDir);
                }
            } else {
                return appDir;
            }  
        }

        appDir.mkdir();
        appDir.setLastModified(app.lastModified());
            
        InputStream istream = new FileInputStream(app);
        JarInputStream jstream = new JarInputStream(istream);

        try {
            explodeJarFromStreamToDir(jstream, appDir);
        } finally {
            jstream.close();
        }

        return appDir;
    }

    public static void explodeJarFromStreamToDir(JarInputStream jstream, File parent) throws IOException {
        JarEntry entry = jstream.getNextJarEntry();
        
        while (entry != null) {
            String name = entry.getName();
            long size = entry.getSize();

            if (entry.isDirectory()) {               
                File f = new File(parent, name);
                f.mkdirs();                
            } else {
                int index = name.lastIndexOf('/');
                File f = new File(parent, name);
                if (index != -1) {
                    String dirName = name.substring(0, index);
                    File fp = new File(parent, dirName);
                    fp.mkdirs();
                }

                byte[] bytes = Utils.streamToByteArray(jstream, (int)size);
                if (name.endsWith(".jar")) {
                    JarInputStream innerJarStream = null;
                    try {
                        f.mkdirs();
                        InputStream bstream = new ByteArrayInputStream(bytes);
                        innerJarStream = new JarInputStream(bstream);
                        explodeJarFromStreamToDir(innerJarStream, f);
                    } finally {
                        innerJarStream.close();
                    }                        
                } else {
                    OutputStream ostream = null;
                    f.createNewFile();
                    try {
                        ostream = new FileOutputStream(f);
                        ostream = new BufferedOutputStream(ostream);
                        ostream.write(bytes);
                    } finally {
                        if (ostream != null) {
                            ostream.close();
                        }
                    }
                }
                long time = entry.getTime();
                f.setLastModified(time);
            }
            
            entry = jstream.getNextJarEntry();
        }
    }
    
    public static boolean deleteFile(File f) {
        if (f.isDirectory()) {            
            File[] files = f.listFiles();
            
            for (File file : files) {
                boolean deleted = deleteFile(file);

                if (!deleted) {
                    return false;
                }
            }           
        }

        return f.delete();
    }
    
}

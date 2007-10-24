/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import com.iona.cxf.container.managed.JMXContainer;
import com.iona.cxf.container.util.ApplicationExploder;
import com.iona.cxf.container.util.Utils;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextAware;

/**
 * This Bean encapsulates the logic for the Spring Container. This Bean is handles the logic for
 * deploying user applications deployed to the specified repository location.
 */

public final class ContainerBean 
    implements JMXContainer, InitializingBean, DisposableBean, ApplicationContextAware {

    private static final Logger LOG = LogUtils.getL7dLogger(ContainerBean.class);
    private static final String WAR_FILE_SUFFIX = ".war";
    private static final String CORRUPTED_JAR_SUFFIX = ".corrupted";
    private File repository;
    private Map<String, Application> applications = new Hashtable<String, Application>();
    private org.springframework.context.ApplicationContext applicationContext;
    private long scanInterval = -1;
    private Thread scanner;
    private volatile boolean destroy;

    public ContainerBean() {

    }

    /**
     * @param repositoryLoc Location of repository where user applications are deployed.
     */
    public ContainerBean(File repositoryLoc) {
        repository = repositoryLoc;
    }

    /**
     * @returns the location of the repository where user applications live.
     */     
    public File getContainerRepository() {
        return repository;
    }

    /**
     * @param dir the location of the repository where user applications live.
     */     
    public void setContainerRepository(File dir) {
        repository = dir;
    }

    public void setScanInterval(long interval) {
        scanInterval = interval;
    }

    public long getScanInterval() {
        return scanInterval;
    }

    /**
     * Used by a Spring <code>ApplicationContext</code> to set the context that instantiated this Bean.
     * This does not need to be set by a user
     */
    public void setApplicationContext(org.springframework.context.ApplicationContext context) {
        applicationContext = context;
    }
    
    /**
     * @returns the <code>ApplicationContext</code> used to instantiate this Bean.
     */
    public org.springframework.context.ApplicationContext getApplicationContext() {
        return applicationContext;
    }  

    /**
     * Launches each of the applications contained in the container repository
     */
    public synchronized void run() {
        scanRepository();

        if (scanInterval > 0 && scanner == null) {
            scanner = new Thread(new Scanner());
            scanner.start();
        }
    }

    /**
     * Stops all applications running in the container but does not stop container
     */
    public synchronized void shutdown() {
        Set<String> keys = applications.keySet();

        Iterator<String> it = keys.iterator();
        String appName = null;

        while (it.hasNext()) {
            try {
                appName = it.next();
                Application ctx = applications.get(appName);
                ctx.stop();
            } catch (Exception ex) {
                LogUtils.log(LOG, Level.SEVERE, "APPLICATION_STOP_FAILED", ex, new Object[] {appName});
            }
        }
    } 

    public void deploy(String location) throws ContainerException {
        String warFileName = null;
        URL url = stringToURL(location);
        String path = url.getPath();
        int index = path.lastIndexOf('/');
                
        if (index >= 0) {
            warFileName = path.substring(index + 1);
        }

        deploy(url, warFileName);
    }

    /**
     * Used to deploy a new application to the container repository. This method copies a war file from 
     * a given URL or File location into the container repository using the specified name for the copied war.
     * If deployment fails, the copied file and its corresponding exploded directory will be deleted.
     * @param location a URL or File location pointing to the application to be deployed.
     * @param warFileName the name of the copied file as it will appear in the container repository, i.e.
     * using the name "test" will cause a file named "test.war" to appear in the container repository
     * @throws ContainerException  if deployment fails
     */    
    public void deploy(String location, String warFileName) throws ContainerException {
        URL url = stringToURL(location);                
        deploy(url, warFileName);
    }

    /**
     * @see #deploy(String location, String warFileName)
     */
    public synchronized void deploy(URL url, String warFileName) throws ContainerException {
        LOG.log(Level.INFO, "APPLICATION_DEPLOY_URL", new Object[] {url});

        if (!warFileName.endsWith(WAR_FILE_SUFFIX)) {
            warFileName += WAR_FILE_SUFFIX;
        }
        
        try {
            File war = new File(repository, warFileName);
            
            if (war.exists()) {
                throw new ContainerException(new Message("WAR_ALREADY_EXISTS", LOG, 
                                                         new Object[] {warFileName}));
            }        
            
            InputStream istream = null;
            byte[] bytes = null;
            
            try {
                istream = url.openStream(); 
                bytes = Utils.streamToByteArray(istream);
            } catch (IOException ioex) {
                throw new ContainerException(ioex);
            } finally {
                if (null != istream) {
                    istream.close();
                }
            }
            
            OutputStream ostream = null;
            try {
                FileOutputStream fout = new FileOutputStream(war);
                ostream = new BufferedOutputStream(fout);
                ostream.write(bytes);
            } catch (IOException ioex2) {
                throw new ContainerException(ioex2);
            } finally {
                ostream.close();
            }

            deployWar(war);

        } catch (IOException ioex3) {
            throw new ContainerException(ioex3);
        }
    }

    void deployWar(File war) throws ContainerException {
        File appDir = null;
        try {
            appDir = ApplicationExploder.explodeApplication(war, repository);
        } catch (IOException ioex) {
            File invalidWar = new File(war.toString() + CORRUPTED_JAR_SUFFIX);
            war.renameTo(invalidWar);
            throw new ContainerException(ioex);
        }
        
        deployApplication(appDir);
    }

    private void deployApplication(File appDir) throws ContainerException {
        try {
            LOG.log(Level.INFO, "EXPLODED_APPLICATION_DIR", new Object[] {appDir});
            Application app = new Application(appDir.getName(), appDir);
            applications.put(app.getName(), app);
            app.start();
        } catch (Exception ex) {
            throw new ContainerException(ex);
        }
    }

    public List<Application> getApplications() {
        return new ArrayList<Application>(applications.values());
    }
    
    /**
     * Stops the specified application based upon its name. This does not remove the application
     * from the container repository.
     * @param name 
     * @throws ContainerException if stopping the applicsation fails.
     */
    public synchronized void stopApplication(String name) throws ContainerException {
        Application app = applications.get(name);
        
        if (app != null) {  
            LOG.log(Level.INFO, "STOPPING_APPLICATION", new Object[] {name});
            app.stop();
        } else {
            throw new ContainerException(new Message("APPLICATION_DOES_NOT_EXIST", LOG, new Object[] {name}));
        }
    }

    /**
     * Starts an application that has previously been deployed and subsequently stopped. 
     * @param name the name of the application to start
     * @throws ContainerException if starting the applicatoin fails.
     */
    public synchronized void startApplication(String name) throws ContainerException {
        Application app = applications.get(name);

        if (app != null) {  
            LOG.log(Level.INFO, "STARTING_APPLICATION", new Object[] {name});
            app.start();
        } else {
            throw new ContainerException(new Message("APPLICATION_DOES_NOT_EXIST", LOG, new Object[] {name}));
        }
    }

    /**
     * Stops and removes an application. This action completely removes an application from the
     * container repository.
     * @param name the name of the application to remove from the container repository.
     * @throws ContainerException if stopping the application fails or if removing artifacts
     * from the container repository fails.
     */
    public synchronized void removeApplication(String name) throws ContainerException {
        Application app = applications.get(name);

        if (app != null) {
            LOG.log(Level.INFO, "REMOVING_APPLICATION", new Object[] {name});
            try {
                applications.remove(name);
                app.stop();
            } catch (Exception ex) {
                throw new ContainerException(ex);
            } finally {                
                File dir = app.getApplicationDirectory();
                String warName = dir.getName() + WAR_FILE_SUFFIX;
                ApplicationExploder.deleteFile(dir);
                File war = new File(repository, warName);

                if (war.exists()) {
                    war.delete();
                }
            }
        } else {
            throw new ContainerException(new Message("APPLICATION_DOES_NOT_EXIST", LOG, new Object[] {name}));
        }
    }

    /**
     * Lists names of all applications that have been deployed. These applications may be in either a start 
     * or stop state.
     * @returns list of application names.
     */
    public synchronized String[] listApplicationNames() {
        Set<String> set = applications.keySet();
        String[] names = set.toArray(new String[set.size()]);

        return names;
    }
    
    /**
     * Lists all services that constitute this application.
     * @param name the name of the application
     * @returns list of expanded qualified service names.
     */
    public String[] listApplicationServices(String name) throws ContainerException {
        Application app = applications.get(name);

        if (app != null) {
            QName[] qnames = app.getServices().toArray(new QName[]{});
            String[] services = new String[qnames.length];
            for (int i = 0; i < qnames.length; i++) {
                services[i] = qnames[i].toString();
            }
            return services;
        } else {
            throw new ContainerException(new Message("APPLICATION_DOES_NOT_EXIST", LOG, new Object[] {name}));
        }
    }

    public synchronized ApplicationState getApplicationState(String name) throws ContainerException {
        Application app = applications.get(name);

        if (app != null) {  
            return app.getState();
        } else {
            throw new ContainerException(new Message("APPLICATION_DOES_NOT_EXIST", LOG, new Object[] {name}));
        }
    }

    
    public synchronized void scanRepository() {
        File[] files = repository.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".war");
                }
            });
        
        if (null != files) { 
            for (File file : files) {
                String fileName = file.getName();
                int i = fileName.indexOf(".war");
                String appName = fileName.substring(0, i);
                Application app = applications.get(appName);

                //Check to see if application has been deployed
                if (null == app) {
                    try {
                        LOG.log(Level.INFO, "DEPLOYING_APPLICATION", new Object[] {appName});
                        deployWar(file);
                    } catch (ContainerException ctex) {
                        LOG.log(Level.SEVERE, ctex.getMessage(), ctex);
                    }
                }
            }
        }
    }    
    /**
     * Bean initialization method.
     */
    public void afterPropertiesSet() throws Exception {
        if (null == repository) {
            throw new ContainerException(new Message("REPOSITORY_LOC_NOT_SET", LOG));
        }

        if (!repository.exists()) {
            throw new ContainerException(new Message("REPOSITORY_LOC_DOES_NOT_EXIST", 
                                                     LOG, new Object[] {repository}));
        }

        run();
    }

    /**
     * Bean destruction method.
     */
    public void destroy() throws Exception {
        LOG.log(Level.INFO, "DESTROYING_CONTAINER", (Object[])null);
        destroy = true;

        if (scanner != null) {
            scanner.join();
        }

        shutdown();
    }

    private URL stringToURL(String location) throws ContainerException {
        URL url = null;

        try {
            url = new URL(location);
        } catch (MalformedURLException mux) {
            File file = new File(location);
            
            if (file.exists()) {
                try {
                    url = file.toURL();
                } catch (MalformedURLException mux2) {
                    throw new ContainerException(new Message("INVALID_LOCATION", LOG, new Object[] {mux}));
                }
            } else {                
                throw new ContainerException(new Message("INVALID_LOCATION", LOG, new Object[] {mux}));
            }
        }

        return url;
    }

    private class Scanner implements Runnable {                

        public void run() {
            while (!destroy) {
                scanRepository();
                
                try {
                    Thread.sleep(scanInterval);
                } catch (InterruptedException iex) {
                    break;
                }
            }            
        }     
    }
    
}

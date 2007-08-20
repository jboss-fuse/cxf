/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container.admin;

import java.util.ArrayList;
import java.util.List;

import com.iona.cxf.container.ApplicationState;
import com.iona.cxf.container.ContainerBean;
import com.iona.cxf.container.ContainerException;

import org.apache.cxf.Bus;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

@javax.jws.WebService(name = "ContainerService",
                      serviceName = "ContainerService", 
                      endpointInterface = "com.iona.cxf.container.admin.ContainerService",
                      targetNamespace = "http://cxf.iona.com/container/admin")
public class ContainerAdminServiceImpl implements ContainerService, InitializingBean, DisposableBean {
    
    private ContainerBean container;

    public ContainerAdminServiceImpl() { 
        
    }

    public List<String> listApplications() {
        String[] names = container.listApplicationNames();
        List<String> list = new ArrayList<String>();
        
        for (String name : names) {
            list.add(name);
        }

        return list;
    }

    public void stopApplication(String name) throws ContainerFault {
        try {
            container.stopApplication(name);
        } catch (ContainerException cex) {
            ContainerFaultType faultType = new ContainerFaultType();
            faultType.setError(cex.getMessage());
            throw new ContainerFault("Stopping application " + name + " failed.", 
                                     faultType, cex);
        }
    }

    public void startApplication(String name) throws ContainerFault {
        try {
            container.startApplication(name);
        } catch (ContainerException cex) {
            ContainerFaultType faultType = new ContainerFaultType();
            faultType.setError(cex.getMessage());
            throw new ContainerFault("Starting application " + name + " failed.", 
                                     faultType, cex);
        }
    }

    public void removeApplication(String name) throws ContainerFault {
        try {
            container.removeApplication(name);
        } catch (ContainerException cex) {
            ContainerFaultType faultType = new ContainerFaultType();
            faultType.setError(cex.getMessage());
            throw new ContainerFault("Removing application " + name + " failed.", 
                                     faultType, cex);
        }
    }

    public void setContainer(ContainerBean c) {
        container = c;
    }
    
    public void shutdown() throws ContainerFault {
        try {
            ApplicationContext context = container.getApplicationContext();
            Bus bus = (Bus)context.getBean("cxf");
            Thread t = new ShutdownThread(bus);
            t.start();
        } catch (Exception cex) {
            ContainerFaultType faultType = new ContainerFaultType();
            faultType.setError(cex.getMessage());
            throw new ContainerFault("Shutting down container failed.", 
                                     faultType, cex);
        }
    }

    public ApplicationStateType getApplicationState(java.lang.String application) 
        throws ContainerFault {
        try {
            ApplicationState state = container.getApplicationState(application);
            
            switch (state) {
            case STARTED:
                return ApplicationStateType.STARTED;
            
            case STOPPED:
                return ApplicationStateType.STOPPED;
                
            default:
                return ApplicationStateType.FAILED;
            } 
        } catch (ContainerException cex) {
            ContainerFaultType faultType = new ContainerFaultType();
            faultType.setError(cex.getMessage());
            throw new ContainerFault("Could not get ApplicationState for application: " + application, 
                                     faultType, cex);
        }
    } 

    public void afterPropertiesSet() throws Exception {
        if (null == container) {
            throw new Exception("Container not set for Bean.");
        }
    }

    public void destroy() throws Exception {

    }

    public class ShutdownThread extends Thread {
        private Bus bus;
        
        public ShutdownThread(Bus b) {
            bus = b;
        }

        public void run() {
            try {
                //Make sure call which started this thread has completed.
                Thread.sleep(5000);
                bus.shutdown(true);
            } catch (InterruptedException inex) {
                //
            }
        }
    }

}

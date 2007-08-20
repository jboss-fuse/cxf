/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;

import com.iona.cxf.container.admin.ContainerService;
import com.iona.cxf.container.admin.ContainerService_Service;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.AbstractToolContainer;
import org.apache.cxf.tools.common.toolspec.ToolRunner;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.CommandLineParser;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;

public final class ContainerLauncher extends AbstractToolContainer {

    private static final Logger LOG =  LogUtils.getL7dLogger(ContainerLauncher.class);
    private static final String SPRING_CONTAINER_CONFIG = "spring_container.xml";
    private static final QName SERVICE_NAME = new QName("http://cxf.iona.com/container/admin", 
                                                        "ContainerService");
    private static final String CONTAINER_WSDL = "/wsdl/container.wsdl";
    private static final String TOOL_NAME = "spring_container";
    private static final String START_STOP_PARAM_ID = "start|stop";
    private static final String CONFIG_LOC_ID = "container-config-loc";
    private static final String START = "start";
    private static final String STOP = "stop";
    private boolean shutdownComplete;


    /**
     * Tool class used to start and stop a Spring Container
     */
    
    public ContainerLauncher(ToolSpec toolspec) throws Exception {
        super(toolspec);
    }


    public static void main(String[] args) {
        try {
            ToolRunner.runTool(ContainerLauncher.class,
                               getResourceAsStream("launchcontainer.xml"),
                               false,
                               args);            
        } catch (Exception ex) {
            System.err.println("Error : " + ex.getMessage());
            System.err.println();
            ex.printStackTrace();
        }
    }

    public void execute(boolean exitOnFinish) throws ToolException {
        try {
            super.execute(exitOnFinish);
            CommandDocument cmdDoc = getCommandDocument();
            
            if (hasInfoOption(cmdDoc)) {
                outputInfo(cmdDoc);
            } else {
                checkParams(cmdDoc);
                String param1 = cmdDoc.getParameter(START_STOP_PARAM_ID);
                if (START.equals(param1)) {
                    LogUtils.log(LOG, Level.INFO, "STARTING_CONTAINER");
                    startContainer(cmdDoc);
                } else if (STOP.equals(param1)) {
                    LogUtils.log(LOG, Level.INFO, "STOPPING_CONTAINER");
                    stopContainer();
                } else {
                    throw new Exception("ContainerLauncher requires start/stop argument.");
                }
            }
        } catch (ToolException ex) {
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(TOOL_NAME, (BadUsageException)ex.getCause());
            } else {
                System.err.println("Error : " + ex.getMessage());
                System.err.println();
                if (isVerboseMode()) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            System.err.println("Error : " + ex.getMessage());
            System.err.println();
            if (isVerboseMode()) {
                ex.printStackTrace();
            }
        }
    }

    private boolean hasInfoOption(CommandDocument cmdDoc) throws ToolException {
        boolean result = false;

        if ((cmdDoc.hasParameter("help")) || (cmdDoc.hasParameter("version"))) {
            result = true;
        }
        return result;
    }
    
    private void outputInfo(CommandDocument cmdDoc) {
        CommandLineParser parser = getCommandLineParser();

        try {
            System.out.println(TOOL_NAME + " " + getUsage());
            System.out.println();
            System.out.println("Options : ");
            System.out.println();
            System.out.println(parser.getDetailedUsage());
            String toolUsage = parser.getToolUsage();
            
            if (toolUsage != null) {
                System.out.println(toolUsage);
                System.out.println();
            }
        } catch (Exception ex) {
            System.err.println("Error : Could not output detailed usage");
            System.err.println();
        }
    }

    private void startContainer(CommandDocument cmd) throws Exception {
        URL url = null;
        String configLoc = cmd.getParameter(CONFIG_LOC_ID);      
        
        if (null != configLoc) {
            try {
                url = new URL(configLoc);
            } catch (MalformedURLException mex) {
                url = getResource(configLoc);
            }
            
            if (null == url) {
                String error = "Could not resolve config location " + configLoc;
                throw new ToolException(error);
            }
        } else {        
            url = getResource(SPRING_CONTAINER_CONFIG);
            if (null == url) {
                String error = "Could not resolve config " + SPRING_CONTAINER_CONFIG;
                throw new ToolException(error);
            }
        }            
        
        LogUtils.log(LOG, Level.INFO, "LOADING_CONTAINER_CONFIG", new Object[] {url});

        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus(url);               
        BusFactory.setDefaultBus(bus);

        BusLifeCycleManager lm = bus.getExtension(BusLifeCycleManager.class);
        if (null != lm) {
            lm.registerLifeCycleListener(new ContainerBusLifeCycleListener());
        }

        System.err.println("Spring Container Ready");

        bus.run();        

        //Wait for postShutdown() to be called before
        //allowing this thread to exit
        while (!shutdownComplete) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException inex) {
                //
            }
        }

        //If this is the main thread then call System.exit() to 
        //keep Jetty non-daemon thread from blocking vm
        if ("main".equals(Thread.currentThread().getName())) {
            System.exit(0);
        }

    }
    
    private void stopContainer() throws Exception {
        URL wsdlURL = getResource(CONTAINER_WSDL);
        ContainerService_Service ss = new ContainerService_Service(wsdlURL, SERVICE_NAME);
        ContainerService port = ss.getContainerServicePort();  
        port.shutdown();
    }

    private void printUsageException(String toolName, BadUsageException ex) {
        if (isVerboseMode()) {
            outputFullCommandLine();
        }

        System.err.println(ex.getMessage());
        System.err.println("Usage : " + TOOL_NAME + " " + ex.getUsage());
        System.err.println();
    }

    public String getUsage() {
        String usage = null;

        try {
            CommandLineParser parser = getCommandLineParser();
            
            if (parser != null) {
                usage = parser.getUsage();
            }
        } catch (Exception ex) {
            usage = "Could not get usage for the tool";
        }

        return usage;
    }

    private void outputFullCommandLine() {
        System.out.print(TOOL_NAME);
        for (int i = 0; i < this.getArgument().length; i++) {
            System.out.print(" " + this.getArgument()[i]);
        }
        System.out.println();
    }

    private void checkParams(CommandDocument cmdDoc) throws ToolException {
        ErrorVisitor errors = new ErrorVisitor();

        if (!cmdDoc.hasParameter(START_STOP_PARAM_ID)) {
            errors.add(new ErrorVisitor.UserError("start or stop state must to be specified for param1."));
            Message msg = new Message("PARAMETER_MISSING", LOG);
            throw new ToolException(msg, new BadUsageException(getUsage(), errors));
        }
    }

    private static InputStream getResourceAsStream(String file) {
        InputStream istream = ContainerLauncher.class.getResourceAsStream(file);

        if (null == istream) {
            istream = ContainerLauncher.class.getResourceAsStream("/" + file);
        }

        return istream;
    }

    private static URL getResource(String name) {
        URL resource = ContainerLauncher.class.getResource("/" + name);

        if (null == resource) {
            resource = ContainerLauncher.class.getResource(name);
        }

        return resource;
    }


    class ContainerBusLifeCycleListener implements BusLifeCycleListener {

        public ContainerBusLifeCycleListener() {

        }

        public void initComplete() {
        }

        public void preShutdown() {
        }

        public void postShutdown() {
            shutdownComplete = true;
        }
    }

}

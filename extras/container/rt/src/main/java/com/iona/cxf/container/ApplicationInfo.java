/**
 *        Copyright (c) 1993-2006 IONA Technologies PLC.
 *                       All Rights Reserved.
 */
package com.iona.cxf.container;

public class ApplicationInfo {
    
    private String name;
    private String type;
    private String containerVersion;
    private String configFile;
    
    public void setContainerVersion(String version) {
        containerVersion = version;
    }
    
    public void setType(String value) {
        type = value;
    }
    
    public void setName(String value) {
        name = value;
    }
    
    public void setConfigFile(String value) {
        configFile = value;
    }

    public String getName() {
        return name;
    }

    public String getConfigFile() {
        return configFile;
    }

    public String getType() {
        return type;
    }
    
    public String getContainerVersion() {
        return containerVersion;
    }
}

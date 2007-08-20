package com.iona.cxf.container;

import java.util.logging.Logger;
import com.iona.cxf.test.greeter.Greeter;
import com.iona.cxf.test.greeter.PingMeFault;
import com.iona.cxf.test.greeter.types.FaultDetail;

@javax.jws.WebService(name = "Greeter",
                      serviceName = "GreeterService", 
                      endpointInterface = "com.iona.cxf.test.greeter.Greeter",
                      targetNamespace = "http://cxf.iona.com/test/greeter")
public class GreeterImpl implements Greeter {

    private static final Logger LOG = 
        Logger.getLogger(GreeterImpl.class.getPackage().getName());
    
    public String greetMe(String me) {
        LOG.info("Executing operation greetMe");
        String retVal = "Hello " + me;

        return retVal;
    }
    
    public String sayHi() {
        LOG.info("Executing operation sayHi");
        return "Bonjour";
    }
    
    public void pingMe() throws PingMeFault {
        FaultDetail faultDetail = new FaultDetail();
        faultDetail.setMajor((short)2);
        faultDetail.setMinor((short)1);
        LOG.info("Executing operation pingMe, throwing PingMeFault exception");
        throw new PingMeFault("PingMeFault raised by server", faultDetail);
    }

    
}

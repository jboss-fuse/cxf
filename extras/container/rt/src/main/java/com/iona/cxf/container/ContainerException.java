/**
 *        Copyright (c) 1993-2007 IONA Technologies PLC.
 *                       All Rights Reserved.
 */

package com.iona.cxf.container;

import org.apache.cxf.common.i18n.Message;

public class ContainerException extends Exception {

    private static final long serialVersionUID = 1L;

    public ContainerException(Message msg) {
        super(msg.toString());
    }

    public ContainerException(Message msg, Throwable t) {
        super(msg.toString(), t);
    }

    public ContainerException(Throwable cause) {
        super(cause);
    }

}

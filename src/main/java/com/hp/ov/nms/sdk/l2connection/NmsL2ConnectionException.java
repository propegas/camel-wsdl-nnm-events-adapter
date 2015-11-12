
package com.hp.ov.nms.sdk.l2connection;

import javax.xml.ws.WebFault;


/**
 * JBossWS Generated Source
 *
 * Generation Date: Tue Jun 19 11:51:55 IST 2007
 *
 * This generated source code represents a derivative work of the input to
 * the generator that produced it. Consult the input for the copyright and
 * terms of use that apply to this source code.
 *
 * JAX-WS Version: 2.0
 *
 */
@WebFault(name = "NmsL2ConnectionFault", targetNamespace = "http://l2connection.sdk.nms.ov.hp.com/")
public class NmsL2ConnectionException extends Exception {
    /**
     * Java type that goes as soapenv:Fault detail element.
     *
     */
    private NmsL2ConnectionFault faultInfo;

    /**
     *
     * @param faultInfo
     * @param message
     */
    public NmsL2ConnectionException(String message, NmsL2ConnectionFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     *
     * @param faultInfo
     * @param message
     * @param cause
     */
    public NmsL2ConnectionException(String message, NmsL2ConnectionFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     *
     * @return
     *     returns fault bean: com.hp.ov.nms.sdk.node.NmsNodeException
     */
    public NmsL2ConnectionFault getFaultInfo() {
        return faultInfo;
    }

}

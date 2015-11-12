package com.hp.ov.nms.sdk.iface;

import java.rmi.Remote;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style = Style.RPC)
public interface NmsInterface extends Remote, NmsInterfaceLocal {
}

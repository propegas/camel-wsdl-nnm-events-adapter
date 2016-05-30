package com.hp.ov.nms.sdk.topology;

public interface NmsTopologyLocal {
    NetworkPathResponse getPath(String startNode, String endNode) throws NmsTopologyException;
}

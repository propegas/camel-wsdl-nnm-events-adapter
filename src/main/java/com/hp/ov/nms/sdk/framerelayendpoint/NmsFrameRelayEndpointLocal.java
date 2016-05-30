package com.hp.ov.nms.sdk.framerelayendpoint;

//

import com.hp.ov.nms.sdk.filter.Filter;

//
public interface NmsFrameRelayEndpointLocal {
    FrameRelayEndpoint[] getFrameRelayEndpoints(Filter filter) throws NmsFrameRelayEndpointException;
}

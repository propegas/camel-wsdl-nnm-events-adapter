package com.hp.ov.nms.sdk.incidentconfig;

import com.hp.ov.nms.sdk.client.SampleClientMBean;


public interface IncidentConfigClientMBean extends SampleClientMBean {
    String listManagementEventIncidentConfig();
    String listRemoteEventIncidentConfig();
    String listSnmpTrapIncidentConfig();
    String listPairwiseIncidentConfig();
}

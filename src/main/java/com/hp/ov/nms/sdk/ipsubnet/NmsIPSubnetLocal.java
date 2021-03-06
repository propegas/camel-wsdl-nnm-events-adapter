package com.hp.ov.nms.sdk.ipsubnet;


import com.hp.ov.nms.sdk.filter.Filter;


public interface NmsIPSubnetLocal {
    IPSubnet[] getIPSubnets(Filter filter) throws NmsIPSubnetException;
    String[] getTenantsByUuids(String[] uuids) throws NmsIPSubnetException;
    void updateNotes(String id,String value) throws NmsIPSubnetException;
}

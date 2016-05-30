package com.hp.ov.nms.sdk.mib;


import com.hp.ov.nms.sdk.filter.Filter;


public interface NmsMibLocal {

    MibVariable[] getMibs(Filter filter) throws NmsMibException;
}

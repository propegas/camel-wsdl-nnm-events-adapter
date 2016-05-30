package com.hp.ov.nms.sdk.topology;

import java.net.SocketOption;

/**
 * Created by vgoryachev on 15.03.2016.
 * Package: com.hp.ov.nms.sdk.topology.
 */
public class NmsPathElement {
    private String label;
    private SocketOption type;
    private String macAddress;
    private boolean discovered;
    private boolean source;
    private boolean destination;
    private String id;
    private String modelClass;

    public SocketOption getType() {
        return type;
    }

    public void setType(SocketOption type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    public boolean isSource() {
        return source;
    }

    public void setSource(boolean source) {
        this.source = source;
    }

    public boolean isDestination() {
        return destination;
    }

    public void setDestination(boolean destination) {
        this.destination = destination;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModelClass() {
        return modelClass;
    }

    public void setModelClass(String modelClass) {
        this.modelClass = modelClass;
    }
}

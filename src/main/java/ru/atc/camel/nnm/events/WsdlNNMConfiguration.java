package ru.atc.camel.nnm.events;

import org.apache.camel.spi.UriParams;

@UriParams
public class WsdlNNMConfiguration {

    private String eventsuri;

    private String wsusername;

    private String wsdlapiurl;

    private int wsdlMaxObjects;

    private int wsdlEventsMaxAgeInDays;

    private String source;

    private String adaptername;

    private int wsdlapiport;

    private long lasttimestamp;

    private String wspassword;

    private int lastid;

    private String eventsdump;

    private String query;

    private int delay;

    private int startindex;

    private String specialEvent = "true";

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getStartindex() {
        return startindex;
    }

    public void setStartindex(int startindex) {
        this.startindex = startindex;
    }

    public String getSpecialEvent() {
        return specialEvent;
    }

    public void setSpecialEvent(String specialEvent) {
        this.specialEvent = specialEvent;
    }

    public String getEventsuri() {
        return eventsuri;
    }

    public void setEventsuri(String eventsuri) {
        this.eventsuri = eventsuri;
    }

    public int getLastid() {
        return lastid;
    }

    public void setLastid(int lastid) {
        this.lastid = lastid;
    }

    public String getWsdlapiurl() {
        return wsdlapiurl;
    }

    public void setWsdlapiurl(String wsdlapiurl) {
        this.wsdlapiurl = wsdlapiurl;
    }

    public String getWsusername() {
        return wsusername;
    }

    public void setWsusername(String wsusername) {
        this.wsusername = wsusername;
    }

    public String getWspassword() {
        return wspassword;
    }

    public void setWspassword(String wspassword) {
        this.wspassword = wspassword;
    }

    public int getWsdlapiport() {
        return wsdlapiport;
    }

    public void setWsdlapiport(int wsdlapiport) {
        this.wsdlapiport = wsdlapiport;
    }

    public long getLasttimestamp() {
        return lasttimestamp;
    }

    public void setLasttimestamp(long lasttimestamp) {
        this.lasttimestamp = lasttimestamp;
    }

    public String getEventsdump() {
        return eventsdump;
    }

    public void setEventsdump(String eventsdump) {
        this.eventsdump = eventsdump;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAdaptername() {
        return adaptername;
    }

    public void setAdaptername(String adaptername) {
        this.adaptername = adaptername;
    }

    public int getWsdlEventsMaxAgeInDays() {
        return wsdlEventsMaxAgeInDays;
    }

    public void setWsdlEventsMaxAgeInDays(int wsdlEventsMaxAgeInDays) {
        this.wsdlEventsMaxAgeInDays = wsdlEventsMaxAgeInDays;
    }

    public int getWsdlMaxObjects() {
        return wsdlMaxObjects;
    }

    public void setWsdlMaxObjects(int wsdlMaxObjects) {
        this.wsdlMaxObjects = wsdlMaxObjects;
    }
}
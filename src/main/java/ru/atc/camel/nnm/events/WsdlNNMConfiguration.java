package ru.atc.camel.nnm.events;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

@UriParams
public class WsdlNNMConfiguration {	
    
	private String eventsuri;
	
	private String wsusername;
	
	private String wsdlapiurl;
	
	private int wsdlapiport;
	
	private long lasttimestamp;
	
	private String wspassword;
	
	private int lastid = 0;
	
	private String eventsdump ;
	
    @UriParam
    private String query;
    
    @UriParam(defaultValue = "60000")
    private int delay = 60000;
    
    @UriParam(defaultValue = "0")
    private int startindex = 0;
    
    @UriParam(defaultValue = "50")
    private int initcount = 50;
    
    @UriParam(defaultValue = "50")
    private int count = 50;
    
    @UriParam(defaultValue = "true")
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

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
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

	public int getInitcount() {
		return initcount;
	}

	public void setInitcount(int initcount) {
		this.initcount = initcount;
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


}
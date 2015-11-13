package ru.atc.camel.nnm.events;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.ParseConversionEvent;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.ov.nms.sdk.filter.BooleanOperator;
import com.hp.ov.nms.sdk.filter.Condition;
import com.hp.ov.nms.sdk.filter.Expression;
import com.hp.ov.nms.sdk.filter.Filter;
import com.hp.ov.nms.sdk.filter.Operator;
import com.hp.ov.nms.sdk.incident.Cia;
//import com.hp.ov.nms.sdk.incident.GetIncidents;
import com.hp.ov.nms.sdk.incident.Incident;
import com.hp.ov.nms.sdk.incident.NmsIncident;
import com.hp.ov.nms.sdk.nodegroup.NmsNodeGroup;
import com.hp.ov.nms.sdk.nodegroup.NodeGroup;

import ru.at_consulting.itsm.event.Event;
import com.hp.ov.nms.sdk.client.SampleClient;

public class WsdlNNMConsumer extends ScheduledPollConsumer {
	
	private static Logger logger = LoggerFactory.getLogger(Main.class);
	
	private WsdlNNMEndpoint endpoint;
	
	public enum PersistentEventSeverity {
	    OK, INFO, WARNING, MINOR, MAJOR, CRITICAL;
		
	    public String value() {
	        return name();
	    }

	    public static PersistentEventSeverity fromValue(String v) {
	        return valueOf(v);
	    }
	}

	public WsdlNNMConsumer(WsdlNNMEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        //this.afterPoll();
        this.setDelay(endpoint.getConfiguration().getDelay());
	}
	
	@Override
	protected int poll() throws Exception {
		
		String operationPath = endpoint.getOperationPath();
		
		if (operationPath.equals("events")) return processSearchEvents();
		
		// only one operation implemented for now !
		throw new IllegalArgumentException("Incorrect operation: " + operationPath);
	}
	
	private int processSearchEvents() throws Exception {
		
		//Long timestamp;
		
		String host = endpoint.getConfiguration().getWsdlapiurl();
		int port = endpoint.getConfiguration().getWsdlapiport();
		String nnmUser = endpoint.getConfiguration().getWsusername();
		String nnmPass = endpoint.getConfiguration().getWspassword();
		
		Condition cond1 = new Condition();
		cond1.setName("lifecycleState");
		cond1.setValue("com.hp.nms.incident.lifecycle.Closed");
		//cond1.setValue("Closed");
		cond1.setOperator(Operator.NE);
		
		Filter[] subFilters=new Filter[]{ cond1 };
		Expression existFilter = new Expression();
		existFilter.setOperator(BooleanOperator.AND);
		existFilter.setSubFilters(subFilters);
		
		SampleClient sampleClient = new SampleClient() ;
		
		sampleClient.setHost(host);
		sampleClient.setPort(port);
		sampleClient.setNnmPass(nnmPass);
		sampleClient.setNnmUser(nnmUser);
		
		NmsIncident nmsincident  ;
		
		nmsincident = sampleClient.getIncidentService();
		
		/*
		NmsNodeGroup nmsnodegroup = sampleClient.getNodeGroupService();
		NodeGroup[] nodegroup = nmsnodegroup.getNodeGroupsByNode("6442451428");
		
		for(int i=0; i < nodegroup.length; i++){

			NodeGroup node = nodegroup[i];
			logger.info("getId: " + node.getId());
			logger.info("getName: " + node.getName());
			logger.info("getUuid: " + node.getUuid());
			logger.info("getStatus: " + node.getStatus().toString());
		}
		*/
		
		//NmsNodeGroup nmsnodegroup2 = sampleClient.getNodeGroupService();
		
		/*
		Condition cond3 = new Condition();
		cond3.setName("s");
		cond3.setValue("21474890549");
		//cond1.setValue("Closed");
		cond3.setOperator(Operator.EQ);
		
		Filter[] subFilters2=new Filter[]{ cond3 };
		Expression existFilter2 = new Expression();
		existFilter2.setOperator(BooleanOperator.AND);
		existFilter2.setSubFilters(subFilters2);
		NodeGroup[] nodegroup3 = nmsnodegroup.getNodeGroups(existFilter2);
		
		for(int i=0; i < nodegroup3.length; i++){

			NodeGroup node = nodegroup3[i];
			logger.info("2 getId: " + node.getId());
			logger.info("2 getName: " + node.getName());
			logger.info("2 getUuid: " + node.getUuid());
			logger.info("2 getStatus: " + node.getStatus().toString());
		}
		
		*/
		//NmsNodeGroup nmsnodegroup = sampleClient.getNodeGroupService();
		
		/*
		String[] nodegroup2 = nmsnodegroup.getMemberIds("15032401563");
		
		for(int i=0; i < nodegroup2.length; i++){

			String node1 = nodegroup2[i];
			logger.info("node: " + node1);

		}
		*/
		
		//node[0].
		//
		
		Incident[] events = null;
		
		//endpoint.getConfiguration().setLasttimestamp(delay);
		//event.getCreated().getTime() / 1000 
		long Lasttimestamp = endpoint.getConfiguration().getLasttimestamp();
		Lasttimestamp = (Lasttimestamp / 1000) * 1000 - 1000;
		logger.info(String.format("**** Saved Lasttimestamp: %d", Lasttimestamp));
		long timestamp = 0;
		
		
		try {
			logger.info(" **** Try to receive Opened Events ");
			timestamp = System.currentTimeMillis();
			events = nmsincident.getIncidents(existFilter);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.debug(" **** Error while receiving Opened Events " );
			logger.debug( e.getMessage() );
			//e.printStackTrace();
		}
		
		logger.info(" **** Received " + events.length + " Opened Events ****");
		
		
		
		// get Old closed events
		Incident[] closed_events = null;
		if (Lasttimestamp == -1000) {
			Lasttimestamp = timestamp;
		}
		Date date = new Date(Lasttimestamp); 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		String formattedDate = sdf.format(date);
			
			Condition cond = new Condition();
			cond.setName("modified");
			cond.setValue("" + Lasttimestamp);
			cond.setOperator(Operator.GE);
			
			Condition cond2 = new Condition();
			cond2.setName("lifecycleState");
			cond2.setValue("com.hp.nms.incident.lifecycle.Closed");
			//cond1.setValue("Closed");
			cond2.setOperator(Operator.EQ);
			
			subFilters=new Filter[]{ cond, cond2 };
			existFilter = new Expression();
			existFilter.setOperator(BooleanOperator.AND);
			existFilter.setSubFilters(subFilters);
			
			try {
				logger.info(" **** Try to receive Closed Events from " + formattedDate + 
						" (" + Lasttimestamp + ")");
				closed_events = nmsincident.getIncidents(existFilter);
				endpoint.getConfiguration().setLasttimestamp(timestamp);
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.debug(" **** Error while receiving Opened Events " );
				logger.debug( e.getMessage() );
				//e.printStackTrace();
			}
			
			logger.info(" **** Received " + closed_events.length + " CLosed Events ****");
				
			//closed_events
		
		Incident[] allevents = (Incident[]) ArrayUtils.addAll(events,closed_events);
		Event genevent = new Event();
		
		for(int i=0; i < allevents.length; i++){

			genevent = genEventObj(allevents[i]);
			
			logger.debug(genevent.toString());
			logger.debug(String.format("%d", allevents[i].getModified().getTime() / 1000));
			
			Cia[] cias = allevents[i].getCias();
			
			//cias[0].
			
			logger.debug(" **** Create Exchange container");
	        Exchange exchange = getEndpoint().createExchange();
	        exchange.getIn().setBody(genevent, Event.class);
	        exchange.getIn().setHeader("EventIdAndStatus", allevents[i].getUuid() + 
	        		"_" + allevents[i].getId() + 
	        		"_" + genevent.getStatus());

	        getProcessor().process(exchange); 
			
		}
		
		
		
		
        
        return 1;
	}
	
	private Event genEventObj( Incident event ) {
		Event genevent = new Event();
		
		String hostName = "";
		hostName = event.getSourceNodeName();
		genevent.setHost(hostName);
		//genevent.setParametr(event.getEventCategory());
		genevent.setObject(event.getSourceName());
		genevent.setCategory("HARDWARE");
		genevent.setExternalid(event.getUuid());
		genevent.setMessage(event.getFormattedMessage());
		genevent.setSeverity(setRightSeverity(event.getSeverity().name()));
		//PersistentEventSeverity.CRITICAL.toString();
		genevent.setStatus(setRightStatus(event.getLifecycleState()));
		genevent.setOrigin(event.getOrigin().name());
		genevent.setParametr(event.getName());
		genevent.setEventCategory(event.getCategory());
		
		genevent.setTimestamp(event.getCreated().getTime() / 1000 );
		genevent.setEventsource("NNM");
		genevent.setService("NNM");
		genevent.setCi(event.getSourceNodeUuid());
		//System.out.println(event.toString());
		
		//logger.info(genevent.toString());
		
		return genevent;
				
	}
	
	/*
	private Device genDeviceObj( RESTNetworkAdvisorDevice device ) {
		Device gendevice = null;
		//Long timestamp;
		
		//String wwn = event.getNodeWwn();
		
		//getDeviceByWwn(wwn, getSavedWStoken());
		
		//gendevice = new Device();
		//gendevice.setHost(event.getSourceName());
		//genevent.setParametr(event.getEventCategory());
		//gendevice.setObject(event.getNodeWwn());
		//gendevice.setCategory("HARDWARE");

		//System.out.println(event.toString());
		
		//logger.info(genevent.toString());
		
		return gendevice;
				
	}
	*/

	private String setRightStatus(String lifecycleState) {
		// TODO Auto-generated method stub
		
		String newstatus;
		/*
		 * Incident.LifecycleState
 “com.hp.nms.incident.lifecycle.Dampened”
 “com.hp.nms.incident.lifecycle.Registered”
 “com.hp.nms.incident.lifecycle.InProgress”
 “com.hp.nms.incident.lifecycle.Completed”
 “com.hp.nms.incident.lifecycle.Closed”
		 */
		switch (lifecycleState) {
    	case "com.hp.nms.incident.lifecycle.Dampened":  newstatus = "OPEN";break;
    	case "om.hp.nms.incident.lifecycle.Registered":  newstatus = "OPEN";break;
    	case "com.hp.nms.incident.lifecycle.InProgress":  newstatus = "OPEN";break;
    	case "com.hp.nms.incident.lifecycle.Completed":  newstatus = "OPEN";break;
    	case "com.hp.nms.incident.lifecycle.Closed":  newstatus = "CLOSED";break;

    	default: newstatus = "OPEN";break;
    	
	}
	logger.debug("***************** severity: " + lifecycleState);
	logger.debug("***************** newseverity: " + newstatus);
	return newstatus;
	}

	/*
	private int getEventId(String key) {
		int id = -1;
		Pattern p = Pattern.compile("(edbid-)(.*)");
		Matcher matcher = p.matcher(key);
		//String output = "";
		if (matcher.matches())
			id = Integer.parseInt(matcher.group(2));
		//System.out.println(matcher.group(2));
		id = Integer.parseInt(matcher.group(2).toString());
		//System.out.println(id);
		return id;
	}
	*/


	public String setRightSeverity(String severity)
	{
		String newseverity = "";
		/*
		 * 
		Severity
 “NORMAL”
 “WARNING”
 “MINOR”
 “MAJOR”
 “CRITICAL”
		 */
		
		
		
		switch (severity) {
        	case "NORMAL":  newseverity = PersistentEventSeverity.OK.name();break;
        	case "WARNING":  newseverity = PersistentEventSeverity.WARNING.name();break;
        	case "MINOR":  newseverity = PersistentEventSeverity.MINOR.name();break;
        	case "MAJOR":  newseverity = PersistentEventSeverity.MAJOR.name();break;
        	case "CRITICAL":  newseverity = PersistentEventSeverity.CRITICAL.name();break;
        	
        	default:  newseverity = PersistentEventSeverity.INFO.name();break;

		}
		logger.debug("***************** severity: " + severity);
		logger.debug("***************** newseverity: " + newseverity);
		return newseverity;
	}
	
	/*
	private int processSearchFeeds() throws Exception {
		
		String query = endpoint.getConfiguration().getQuery();
		String uri = String.format("login?query=%s", query);
		JsonObject json = performGetRequest(uri);
		
		//JsonArray feeds = (JsonArray) json.get("results");
		JsonArray feeds = (JsonArray) json.get("ServerName");
		List<Feed2> feedList = new ArrayList<Feed2>();
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		for (JsonElement f : feeds) {
			//logger.debug(gson.toJson(i));
			Feed2 feed = gson.fromJson(f, Feed2.class);
			feedList.add(feed);		
		}		
		
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(feedList, ArrayList.class);
        getProcessor().process(exchange); 
        
        return 1;
	}
	*/

}
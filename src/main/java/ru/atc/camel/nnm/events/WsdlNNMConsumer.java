package ru.atc.camel.nnm.events;

import com.hp.ov.nms.sdk.client.SampleClient;
import com.hp.ov.nms.sdk.filter.*;
import com.hp.ov.nms.sdk.incident.Incident;
import com.hp.ov.nms.sdk.incident.NmsIncident;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.at_consulting.itsm.event.Event;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

//import com.hp.ov.nms.sdk.incident.GetIncidents;
//import ru.atc.camel.nnm.devices.WsdlNNMConsumer.PersistentEventSeverity;
//import ru.atc.camel.nnm.devices.WsdlNNMConsumer.PersistentEventSeverity;
//import ru.atc.camel.nnm.devices.WsdlNNMConsumer.PersistentEventSeverity;

public class WsdlNNMConsumer extends ScheduledPollConsumer {

    private static Logger logger = LoggerFactory.getLogger(Main.class);
    private static WsdlNNMEndpoint endpoint;
    private String[] openids = {null};

    public WsdlNNMConsumer(WsdlNNMEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        WsdlNNMConsumer.endpoint = endpoint;
        //this.afterPoll();
        this.setTimeUnit(TimeUnit.MINUTES);
        this.setInitialDelay(0);
        this.setDelay(endpoint.getConfiguration().getDelay());
    }

    public static void genHeartbeatMessage(Exchange exchange) {
        // TODO Auto-generated method stub
        long timestamp = System.currentTimeMillis();
        timestamp = timestamp / 1000;
        //String textError = "Возникла ошибка при работе адаптера: ";
        Event genevent = new Event();
        genevent.setMessage("Сигнал HEARTBEAT от адаптера");
        genevent.setEventCategory("ADAPTER");
        genevent.setObject("HEARTBEAT");
        genevent.setSeverity(PersistentEventSeverity.OK.name());
        genevent.setTimestamp(timestamp);
        genevent.setEventsource(String.format("%s", endpoint.getConfiguration().getAdaptername()));


        logger.info(" **** Create Exchange for Heartbeat Message container");
        //Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(genevent, Event.class);

        exchange.getIn().setHeader("Timestamp", timestamp);
        exchange.getIn().setHeader("queueName", "Heartbeats");
        exchange.getIn().setHeader("Type", "Heartbeats");
        exchange.getIn().setHeader("Source", "NNM_EVENT_ADAPTER");

        try {
            //Processor processor = getProcessor();
            //.process(exchange);
            //processor.process(exchange);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

    @Override
    protected int poll() throws Exception {

        String operationPath = endpoint.getOperationPath();

        if (operationPath.equals("events")) {
            //beforePoll(10000);
            return processSearchEvents();
        }

        // only one operation implemented for now !
        throw new IllegalArgumentException("Incorrect operation: " + operationPath);
    }

    @Override
    public long beforePoll(long timeout) throws Exception {

        logger.info("*** Before Poll!!!");
        // only one operation implemented for now !
        //throw new IllegalArgumentException("Incorrect operation: ");

        //send HEARTBEAT
        genHeartbeatMessage(getEndpoint().createExchange());

        return timeout;
    }

    private int processSearchEvents() {

        try {

            String host = endpoint.getConfiguration().getWsdlapiurl();
            int port = endpoint.getConfiguration().getWsdlapiport();
            String nnmUser = endpoint.getConfiguration().getWsusername();
            String nnmPass = endpoint.getConfiguration().getWspassword();

            SampleClient sampleClient = new SampleClient();
            sampleClient.setHost(host);
            sampleClient.setPort(port);
            sampleClient.setNnmPass(nnmPass);
            sampleClient.setNnmUser(nnmUser);

            // get Old closed events
            Incident[] closed_events = getClosedEventsById(sampleClient);

            // get All new (Open) events
            Incident[] events = getOpenEvents(sampleClient);

            Incident[] allevents = (Incident[]) ArrayUtils.addAll(events, closed_events);
            Event genevent = new Event();

            for (Incident allevent : allevents) {

                //logger.info("ID: " +  allevents[i].getId());
                //allevents[i].getCreated().getTime()
                //logger.info(String.format("TimeCreated: %d", allevents[i].getModified().getTime()));

                logger.debug(String.format("%d", allevent.getModified().getTime() / 1000));

                genevent = genEventObj(allevent);

                logger.debug(genevent.toString());
                logger.debug(String.format("%d", allevent.getModified().getTime() / 1000));

                //Cia[] cias = allevents[i].getCias();

                //cias[0].

                logger.debug(" **** Create Exchange container");
                Exchange exchange = getEndpoint().createExchange();
                exchange.getIn().setBody(genevent, Event.class);
                exchange.getIn().setHeader("EventIdAndStatus", allevent.getUuid() +
                        "_" + allevent.getId() +
                        "_" + genevent.getStatus());

                getProcessor().process(exchange);

            }

        } catch (Exception e) { //send error message to the same queue
            logger.error(String.format("Error while execution: %s ", e));
            genErrorMessage(e.getMessage());
            return 0;
        }

        return 1;
    }

    private void genErrorMessage(String message) {
        long timestamp = System.currentTimeMillis();
        timestamp = timestamp / 1000;
        String textError = "Возникла ошибка при работе адаптера: ";
        Event genevent = new Event();
        genevent.setMessage(textError + message);
        genevent.setEventCategory("ADAPTER");
        genevent.setSeverity(PersistentEventSeverity.CRITICAL.name());
        genevent.setTimestamp(timestamp);
        genevent.setEventsource(String.format("%s", endpoint.getConfiguration().getAdaptername()));
        genevent.setStatus("OPEN");
        genevent.setHost("adapter");

        logger.info(" **** Create Exchange for Error Message container");
        Exchange exchange = getEndpoint().createExchange();
        exchange.getIn().setBody(genevent, Event.class);

        exchange.getIn().setHeader("EventIdAndStatus", "Error_" + timestamp);
        exchange.getIn().setHeader("Timestamp", timestamp);
        exchange.getIn().setHeader("queueName", "Events");
        exchange.getIn().setHeader("Type", "Error");

        try {
            getProcessor().process(exchange);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private Incident[] getClosedEventsById(SampleClient sampleClient) throws Exception {
        // TODO Auto-generated method stub

        Incident[] closed_events = {};
        Incident[] allevents = {};
        /*
        if (Lasttimestamp == -1000) {
			Lasttimestamp = timestamp;
		}

		Date date = new Date(Lasttimestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		String formattedDate = sdf.format(date);

		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String formattedDate1 = sdf1.format(date);
		*/
        NmsIncident nmsincident = null;

        nmsincident = sampleClient.getIncidentService();

        int event_count = 0;
        for (int i = 0; i < openids.length; i++) {
            Condition cond = new Condition();
            cond.setName("id");
            cond.setValue(openids[i]);
            cond.setOperator(Operator.EQ);

            Condition cond2 = new Condition();
            cond2.setName("lifecycleState");
            cond2.setValue("com.hp.nms.incident.lifecycle.Closed");
            //cond1.setValue("Closed");
            cond2.setOperator(Operator.EQ);

            Filter[] subFilters = new Filter[]{cond, cond2};
            Expression existFilter = new Expression();
            existFilter.setOperator(BooleanOperator.AND);
            existFilter.setSubFilters(subFilters);

            logger.debug(" **** Try to receive Closed Events ***** ");

            try {
                logger.debug(" **** Try to receive Closed Events for " + openids[i]);
                closed_events = nmsincident.getIncidents(existFilter);
                //endpoint.getConfiguration().setLasttimestamp(timestamp);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                logger.error(" **** Error while receiving Opened Events ");
                //String.format("Error while SQL execution: %s ", e);
                throw new Error("Error while receiving Opened Events. " + e.getMessage());
                //e.printStackTrace();
            }

            if (closed_events.length > 0) {
                event_count++;
                allevents = (Incident[]) ArrayUtils.addAll(allevents, closed_events);
            }
            logger.debug(" **** 1Received " + closed_events.length + " CLosed Events ****");


			/*
            String eventsdump = endpoint.getConfiguration().getEventsdump();
			logger.info(String.format("**** eventsdump: %s", eventsdump));

			if (eventsdump.compareTo("true") == 0 ){
				logger.info(String.format("**** eventsdump: %s", eventsdump));
				dumpEvents(closed_events, "closed", formattedDate1);
			}
			*/
        }

        logger.info(" **** Received " + event_count + " (" + allevents.length + ") CLosed Events ****");


        return allevents;
    }

    private Incident[] getOpenEvents(SampleClient sampleClient) {
        // TODO Auto-generated method stub

        Constraint cons1 = new Constraint();
        cons1.setName("maxObjects");
        cons1.setValue(String.format("%d", endpoint.getConfiguration().getWsdlMaxObjects()));

        Condition cond1 = new Condition();
        cond1.setName("lifecycleState");
        cond1.setValue("com.hp.nms.incident.lifecycle.Closed");
        cond1.setOperator(Operator.NE);

        int maxEventsAgeInDays = endpoint.getConfiguration().getWsdlEventsMaxAgeInDays();
        long epoch = System.currentTimeMillis();
        long epochInPast = epoch - maxEventsAgeInDays * 24 * 60 * 60 * 1000;
        logger.info(String.format("Use lastOccurrenceTime = %d", epochInPast));

        Condition cond2 = new Condition();
        cond2.setName("lastOccurrenceTime");
        cond2.setValue(String.valueOf(epochInPast));
        cond2.setOperator(Operator.GE);

        Filter[] subFilters = new Filter[]{cond2, cond1, cons1};
        Expression existFilter = new Expression();
        existFilter.setOperator(BooleanOperator.AND);
        existFilter.setSubFilters(subFilters);

        NmsIncident nmsincident;

        nmsincident = sampleClient.getIncidentService();

        String eventsdump = endpoint.getConfiguration().getEventsdump();
        //logger.info(String.format("**** eventsdump: %s", eventsdump));

        Incident[] events;

        try {
            logger.info(" **** Try to receive Opened Events ");
            //timestamp = System.currentTimeMillis();
            events = nmsincident.getIncidents(existFilter);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(" **** Error while receiving Opened Events: " + e);
            throw new RuntimeException("Error while receiving Opened Events. " + e.getMessage());
        }

        logger.info(" **** Received " + events.length + " Opened Events ****");

        logger.debug(" **** Saving Received opend events's IDs ****");

        openids = new String[]{};
        for (int i = 0; i < events.length; i++) {
            openids = (String[]) ArrayUtils.add(openids, events[i].getId());
            logger.debug(" **** Saving ID: " + events[i].getId());
        }

        logger.info(" **** Saved " + openids.length + " Opened Events ****");

        return events;
    }

    private void dumpEvents(Incident[] events, String type, String formattedDate) {
        // TODO Auto-generated method stub
        logger.info(" **** Dumping " + type + " Events ****");
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(String.format("%s_Events_%s", type, formattedDate), true));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        String text;
        for (int i = 0; i < events.length; i++) {
            text = toStrings(events[i]);

            try {
                // APPEND MODE SET HERE
                if (bw != null) {
                    bw.write(text);
                }
                bw.newLine();
                bw.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {

                // always close the file


            } // end try/catch/finally


        }

        try {
            if (bw != null) {
                bw.flush();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            // always close the file
            if (bw != null) try {
                bw.close();
            } catch (IOException ioe2) {
                // just ignore it
            }
        }

    }

    private String toStrings(Incident incident) {
        // TODO Auto-generated method stub
        String text = "******";
        text = text + "\ncreated: " + incident.getCreated();
        text = text + "\nduplicateCount: " + incident.getDuplicateCount();
        text = text + "\nlastOccurrenceTime: " + incident.getLastOccurrenceTime();
        text = text + "\nmodified: " + incident.getModified();
        text = text + "\nformattedMessage: " + incident.getFormattedMessage();
        text = text + "\nid: " + incident.getId();
        text = text + "\nuuid: " + incident.getUuid();
        text = text + "\nseverity: " + incident.getSeverity().name();
        text = text + "\nsourceNodeName: " + incident.getSourceNodeName();
        text = text + "\nlifecycleState: " + incident.getLifecycleState();

        text = text + "\n******\n";

        return text;
    }

    private Event genEventObj(Incident event) {
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

        genevent.setTimestamp(event.getCreated().getTime() / 1000);

        String source = endpoint.getConfiguration().getSource();
        genevent.setEventsource(source);
        genevent.setService("NNM");
        genevent.setCi(String.format("%s:%s", endpoint.getConfiguration().getSource(), event.getSourceNodeUuid()));
        //System.out.println(event.toString());

        //logger.info(genevent.toString());

        return genevent;

    }

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
            case "com.hp.nms.incident.lifecycle.Dampened":
                newstatus = "OPEN";
                break;
            case "om.hp.nms.incident.lifecycle.Registered":
                newstatus = "OPEN";
                break;
            case "com.hp.nms.incident.lifecycle.InProgress":
                newstatus = "OPEN";
                break;
            case "com.hp.nms.incident.lifecycle.Completed":
                newstatus = "OPEN";
                break;
            case "com.hp.nms.incident.lifecycle.Closed":
                newstatus = "CLOSED";
                break;

            default:
                newstatus = "OPEN";
                break;

        }
        logger.debug("***************** severity: " + lifecycleState);
        logger.debug("***************** newseverity: " + newstatus);
        return newstatus;
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

    public String setRightSeverity(String severity) {
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
            case "NORMAL":
                newseverity = PersistentEventSeverity.OK.name();
                break;
            case "WARNING":
                newseverity = PersistentEventSeverity.WARNING.name();
                break;
            case "MINOR":
                newseverity = PersistentEventSeverity.MINOR.name();
                break;
            case "MAJOR":
                newseverity = PersistentEventSeverity.MAJOR.name();
                break;
            case "CRITICAL":
                newseverity = PersistentEventSeverity.CRITICAL.name();
                break;

            default:
                newseverity = PersistentEventSeverity.INFO.name();
                break;

        }
        logger.debug("***************** severity: " + severity);
        logger.debug("***************** newseverity: " + newseverity);
        return newseverity;
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


    public enum PersistentEventSeverity {
        OK, INFO, WARNING, MINOR, MAJOR, CRITICAL;

        public static PersistentEventSeverity fromValue(String v) {
            return valueOf(v);
        }

        public String value() {
            return name();
        }
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
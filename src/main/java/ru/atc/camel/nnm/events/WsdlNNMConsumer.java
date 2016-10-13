package ru.atc.camel.nnm.events;

import com.hp.ov.nms.sdk.client.SampleClient;
import com.hp.ov.nms.sdk.filter.BooleanOperator;
import com.hp.ov.nms.sdk.filter.Condition;
import com.hp.ov.nms.sdk.filter.Constraint;
import com.hp.ov.nms.sdk.filter.Expression;
import com.hp.ov.nms.sdk.filter.Filter;
import com.hp.ov.nms.sdk.filter.Operator;
import com.hp.ov.nms.sdk.incident.Incident;
import com.hp.ov.nms.sdk.incident.NmsIncident;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.atc.adapters.type.Event;

import java.util.concurrent.TimeUnit;

import static ru.atc.adapters.message.CamelMessageManager.genAndSendErrorMessage;
import static ru.atc.adapters.message.CamelMessageManager.genHeartbeatMessage;
import static ru.atc.adapters.type.Event.PersistentEventSeverity;

public class WsdlNNMConsumer extends ScheduledPollConsumer {

    private static final Logger logger = LoggerFactory.getLogger("mainLogger");
    private static final Logger loggerErrors = LoggerFactory.getLogger("errorsLogger");
    private static WsdlNNMEndpoint endpoint;
    private String[] openids = {null};

    public WsdlNNMConsumer(WsdlNNMEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        WsdlNNMConsumer.endpoint = endpoint;
        this.setTimeUnit(TimeUnit.MINUTES);
        this.setInitialDelay(0);
        this.setDelay(endpoint.getConfiguration().getDelay());
    }

    @Override
    protected int poll() throws Exception {

        String operationPath = endpoint.getOperationPath();

        if ("events".equals(operationPath)) {
            return processSearchEvents();
        }

        // only one operation implemented for now !
        throw new IllegalArgumentException("Incorrect operation: " + operationPath);
    }

    @Override
    public long beforePoll(long timeout) throws Exception {

        logger.info("*** Before Poll!!!");

        //send HEARTBEAT
        genHeartbeatMessage(getEndpoint().createExchange(), endpoint.getConfiguration().getAdaptername());

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
            Incident[] closedEvents = getClosedEventsById(sampleClient);

            // get All new (Open) events
            Incident[] events = getOpenEvents(sampleClient);

            Incident[] allEvents = (Incident[]) ArrayUtils.addAll(events, closedEvents);
            Event genEvent;

            for (Incident event : allEvents) {

                logger.debug(String.format("%d", event.getModified().getTime() / 1000));

                genEvent = genEventObj(event);

                logger.debug(genEvent.toString());
                logger.debug(String.format("%d", event.getModified().getTime() / 1000));

                logger.debug(" **** Create Exchange container");
                Exchange exchange = getEndpoint().createExchange();
                exchange.getIn().setBody(genEvent, Event.class);
                exchange.getIn().setHeader("EventIdAndStatus", event.getUuid() +
                        "_" + event.getId() +
                        "_" + genEvent.getStatus());

                getProcessor().process(exchange);

            }

        } catch (Exception e) { //send error message to the same queue
            loggerErrors.error(String.format("Error while execution: %s ", e));
            logger.error(String.format("Error while execution: %s ", e));
            genErrorMessage(e.getMessage());
            return 0;
        }

        return 1;
    }

    private void genErrorMessage(String message) {
        genAndSendErrorMessage(this, message, new RuntimeException("No additional exception's text."),
                endpoint.getConfiguration().getAdaptername());
    }

    private void genErrorMessage(String message, Exception exception) {
        genAndSendErrorMessage(this, message, exception,
                endpoint.getConfiguration().getAdaptername());
    }

    private Incident[] getClosedEventsById(SampleClient sampleClient) throws Exception {

        Incident[] closedEvents;
        Incident[] allEvents = {};

        NmsIncident nmsincident;

        nmsincident = sampleClient.getIncidentService();

        int totalClosedEventCount = 0;
        for (String openid : openids) {
            Condition cond = new Condition();
            cond.setName("id");
            cond.setValue(openid);
            cond.setOperator(Operator.EQ);

            Condition cond2 = new Condition();
            cond2.setName("lifecycleState");
            cond2.setValue("com.hp.nms.incident.lifecycle.Closed");
            cond2.setOperator(Operator.EQ);

            Filter[] subFilters = new Filter[]{cond, cond2};
            Expression existFilter = new Expression();
            existFilter.setOperator(BooleanOperator.AND);
            existFilter.setSubFilters(subFilters);

            logger.debug(" **** Try to receive Closed Events ***** ");

            try {
                logger.debug(" **** Try to receive Closed Events for " + openid);
                closedEvents = nmsincident.getIncidents(existFilter);

            } catch (Exception e) {
                loggerErrors.error(" **** Error while receiving Closed Events ", e);
                logger.error(" **** Error while receiving Closed Events ", e);
                throw new RuntimeException("Error while receiving Closed Events. " + e.getMessage());
            }

            if (closedEvents.length > 0) {
                totalClosedEventCount++;
                allEvents = (Incident[]) ArrayUtils.addAll(allEvents, closedEvents);
            }
            logger.debug(String.format(" **** Received %d Closed Events ****", closedEvents.length));

        }

        logger.info(String.format(" **** Received total %d (%d) Closed Events ****", totalClosedEventCount, allEvents.length));

        return allEvents;
    }

    private Incident[] getOpenEvents(SampleClient sampleClient) {

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

        Incident[] events;

        try {
            logger.info(" **** Try to receive Opened Events ");
            events = nmsincident.getIncidents(existFilter);

        } catch (Exception e) {
            loggerErrors.error(" **** Error while receiving Opened Events: ", e);
            logger.error(" **** Error while receiving Opened Events: ", e);
            throw new RuntimeException("Error while receiving Opened Events. " + e.getMessage());
        }

        logger.info(" **** Received " + events.length + " Opened Events ****");

        logger.debug(" **** Saving Received opened events IDs ****");

        openids = new String[]{};
        for (Incident event : events) {
            openids = (String[]) ArrayUtils.add(openids, event.getId());
            logger.debug(" **** Saving ID: " + event.getId());
        }

        logger.info(" **** Saved " + openids.length + " Opened Events ****");

        return events;
    }

    private Event genEventObj(Incident event) {
        Event genEvent = new Event();

        String hostName;
        hostName = event.getSourceNodeName();
        genEvent.setHost(hostName);
        genEvent.setObject(event.getSourceName());
        genEvent.setCategory("HARDWARE");
        genEvent.setExternalid(event.getUuid());
        genEvent.setMessage(event.getFormattedMessage());
        genEvent.setSeverity(setRightSeverity(event.getSeverity().name()));

        genEvent.setStatus(setRightStatus(event.getLifecycleState()));
        genEvent.setOrigin(event.getOrigin().name());
        genEvent.setParametr(event.getName());
        genEvent.setEventCategory(event.getCategory());

        genEvent.setTimestamp(event.getCreated().getTime() / 1000);

        String source = endpoint.getConfiguration().getSource();
        genEvent.setEventsource(source);
        genEvent.setService("NNM");
        genEvent.setCi(String.format("%s:%s", endpoint.getConfiguration().getSource(), event.getSourceNodeUuid()));

        logger.debug(genEvent.toString());

        return genEvent;

    }

    private String setRightStatus(String lifecycleState) {

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

    public String setRightSeverity(String severity) {
        String newseverity;
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

}
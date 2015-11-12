/*
 * (C) Copyright 2005 Hewlett-Packard Development Company, L.P.
 *
 *  This software is the confidential and proprietary information of
 *  Hewlett-Packard. ("Confidential Information").  You shall not
 *  disclose such Confidential Information and shall use it only in
 *  accordance with the terms of the license agreement you entered into
 *  with Hewlett-Packard.
 *
 *  Hewlett-Packard makes no representations or warranties about the
 *  suitability of the software, either express or implied, including
 *  but not limited to the implied warranties of merchantability,
 *  fitness for a particular purpose, or non-infringement. Hewlett-Packard
 *  shall not be liable for any damages suffered by licensee as a result
 *  of using, modifying or distributing this software or its derivatives.
 *
 *  Created on Nov 7, 2006
 */
package com.hp.ov.nms.sdk.client.notification;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.addressing.AddressingBuilder;
import javax.xml.ws.addressing.AddressingProperties;
import javax.xml.ws.addressing.EndpointReference;
import javax.xml.ws.addressing.JAXWSAConstants;

import org.jboss.ws.core.StubExt;
import org.jboss.ws.core.soap.SOAPElementImpl;
import org.jboss.ws.extensions.addressing.AddressingClientUtil;
import org.jboss.ws.extensions.eventing.EventingConstants;
import org.jboss.ws.extensions.eventing.jaxws.AttributedURIType;
import org.jboss.ws.extensions.eventing.jaxws.DeliveryType;
import org.jboss.ws.extensions.eventing.jaxws.EndpointReferenceType;
import org.jboss.ws.extensions.eventing.jaxws.EventSourceEndpoint;
import org.jboss.ws.extensions.eventing.jaxws.FilterType;
import org.jboss.ws.extensions.eventing.jaxws.GetStatus;
import org.jboss.ws.extensions.eventing.jaxws.GetStatusResponse;
import org.jboss.ws.extensions.eventing.jaxws.Renew;
import org.jboss.ws.extensions.eventing.jaxws.RenewResponse;
import org.jboss.ws.extensions.eventing.jaxws.Subscribe;
import org.jboss.ws.extensions.eventing.jaxws.SubscribeResponse;
import org.jboss.ws.extensions.eventing.jaxws.SubscriptionManagerEndpoint;
import org.jboss.ws.extensions.eventing.jaxws.Unsubscribe;

/**
 * A subscriber helper for NNM notifications.
 *
 * @author Rocky
 *
 */
public class NnmSubscriber {
    private final static Logger log = Logger.getLogger(NnmSubscriber.class.getName());
    private final long NNM_SUBSCRIBE_TIME=EventingConstants.MAX_LEASE_TIME;
    private final long CHECK_DELAY=60*1000L;
    private final long RENEW_DELAY=NNM_SUBSCRIBE_TIME-NNM_SUBSCRIBE_TIME/2;//Renew 3/4s before expire interval.
    private final long CHECK_INTERVAL=60*1000L;//Check subscription every minute.

    public final static String nnmNotificationNamespace="http://notification.sdk.nms.ov.hp.com/";
    //Incident events
    public final static String incidentEventSource="IncidentNotificationSource";
    public final static String incidentEventContext="nms-sdk-notify";
    public final static String incidentEventNamespace=nnmNotificationNamespace+incidentEventContext;
    public final static String incidentEventSourceUri=incidentEventNamespace+"/"+incidentEventSource;

    //Node events
    public final static String nodeEventSource="NodeNotificationSource";
    public final static String nodeEventContext="nms-sdk-node-notify";
    public final static String nodeEventNamespace=nnmNotificationNamespace+nodeEventContext;
    public final static String nodeEventSourceUri=nodeEventNamespace+"/"+nodeEventSource;

    //SNMP events
    public final static String snmpEventSource="SnmpNotificationSource";
    public final static String snmpEventContext="nms-sdk-snmp-notify";
    public final static String snmpEventNamespace=nnmNotificationNamespace+snmpEventContext;
    public final static String snmpEventSourceUri=snmpEventNamespace+"/"+snmpEventSource;

    //Node Security Change events
    public final static String nodeSecurityChangeEventSource="NodeSecurityChangeNotificationSource";
    public final static String nodeSecurityChangeEventContext="nms-sdk-node-security-change-notify";
    public final static String nodeSecurityChangeEventNamespace=nnmNotificationNamespace+nodeSecurityChangeEventContext;
    public final static String nodeSecurityChangeEventSourceUri=nodeSecurityChangeEventNamespace+"/"+nodeSecurityChangeEventSource;

    //Node Tenant Change events
    public final static String nodeTenantChangeEventSource="NodeTenantChangeNotificationSource";
    public final static String nodeTenantChangeEventContext="nms-sdk-node-tenant-change-notify";
    public final static String nodeTenantChangeEventNamespace=nnmNotificationNamespace+nodeTenantChangeEventContext;
    public final static String nodeTenantChangeEventSourceUri=nodeTenantChangeEventNamespace+"/"+nodeTenantChangeEventSource;



    private String eventType=null;
    private String eventSourceURI=null;
    private String eventContext=null;
    private String ejbEP=null;
    private String filter=null;
    private String nnmURL=null;
    private String nnmUser=null;
    private String nnmPass=null;
    private String sinkURL=null;

    private boolean enabled=false;
    private EventSourceEndpoint subscriptionPort = null;
    private SubscriptionManagerEndpoint managementPort = null;
    private SubscribeResponse subscribeResponse=null;
    private DeliveryType delivery = null;
    private SubscriptionChecker subscriptionChecker=null;
    private SubscriptionRenewer subscriptionRenewer=null;

    public NnmSubscriber(String eventType,String eventSink) {
        this.eventType=eventType;
        ejbEP=eventSink;
        if(eventType.equals(incidentEventSource)) {
            eventSourceURI=incidentEventSourceUri;
            eventContext=incidentEventContext;
        } else if(eventType.equals(nodeEventSource)) {
            eventSourceURI=nodeEventSourceUri;
            eventContext=nodeEventContext;
        } else if(eventType.equals(snmpEventSource)) {
            eventSourceURI=snmpEventSourceUri;
            eventContext=snmpEventContext;
        } else if(eventType.equals(nodeSecurityChangeEventSource)) {
            eventSourceURI=nodeSecurityChangeEventSourceUri;
            eventContext=nodeSecurityChangeEventContext;
        } else if(eventType.equals(nodeTenantChangeEventSource)) {
            eventSourceURI=nodeTenantChangeEventSourceUri;
            eventContext=nodeTenantChangeEventContext;
        }
    }

    public NnmSubscriber(String eventType,String eventSourceURI,String context,String eventSink) {
        this.eventType=eventType;
        this.eventSourceURI=eventSourceURI;
        ejbEP=eventSink;
        eventContext=context;
    }

    public void setWSURLSource(String url) {
        nnmURL=url;
    }

    public String getWSURLSource() {
        return nnmURL;
    }

    public void setWSURLTarget(String url) {
        sinkURL=url;
    }

    public String getWSURLTarget() {
        return sinkURL;
    }

    public String getPassword() {
        return nnmPass;
    }

    public void setPassword(String password) {
        nnmPass=password;
    }

    public String getUser() {
        return nnmUser;
    }

    public void setUser(String user) {
        nnmUser=user;
    }

    public String subscribe() throws Exception {
        return subscribeWithFilter(null);
    }

    public String subscribeWithFilter(String filter) throws Exception {
        unsubscribe();
        if (subscriptionPort == null || managementPort == null) {
//            URL wsdlURL = new URL(nnmURL+eventContext+"/manage?wsdl");
            URL wsdlURL=Class.forName("com.hp.ov.nms.sdk.client.notification.NnmSubscriber").getResource("manage"+eventType+".wsdl");
            wsdlURL = new URL(wsdlURL.toExternalForm().replaceAll(" ", "%20"));
            QName defaultServiceName = new QName("http://schemas.xmlsoap.org/ws/2004/08/eventing", "EventingService");

            Service service = Service.create(wsdlURL, defaultServiceName);

            subscriptionPort = service.getPort(EventSourceEndpoint.class);
            managementPort = service.getPort(SubscriptionManagerEndpoint.class);

            ((StubExt)subscriptionPort).setConfigName("Standard WSAddressing Client");
            ((StubExt)managementPort).setConfigName("Standard WSAddressing Client");
        }
        if ("".equals(filter)) {
            filter = null;
        }
        this.filter=filter;
        subscribeResponse = doSubscribe(filter);
        resetTimers();
        return getSubscriptionDetails();
    }

    public void unsubscribe() {
        killTimers();
        if (subscriptionPort != null && managementPort != null && subscribeResponse != null) {
            AddressingProperties unsubscribeProps = buildFollowupProperties(subscribeResponse,
                EventingConstants.UNSUBSCRIBE_ACTION, eventSourceURI);
            setRequestProperties((BindingProvider) managementPort,unsubscribeProps);
            authenticateService((BindingProvider) managementPort,"manage");

            managementPort.unsubscribeOp(new Unsubscribe());
            log.log(Level.FINE, "NNM un-subscribed");
        }
        subscriptionPort=null;
        managementPort=null;
    }

    public String getSubscriptionDetails() {
        EndpointReferenceType managerEPR = subscribeResponse.getSubscriptionManager();
        String subscriptionID = subscribeResponse.getSubscriptionId();

        String results = "SubscriptionManager: " + managerEPR.getAddress();
        results += "\nSubscriptionID: " + subscriptionID;
        results += "\nExpires: " + subscribeResponse.getExpires();
        results+="\nNotify To: "+delivery.getNotifyTo().toString();

        log.log(Level.FINER, results);
        return results;
    }

    public synchronized boolean isSubscriptionOk() {
        boolean ok=false;
        try {
            AddressingProperties requestProps = buildFollowupProperties(
                subscribeResponse, EventingConstants.GET_STATUS_ACTION,eventSourceURI.toString());
            setRequestProperties((BindingProvider) managementPort, requestProps);

            Date expectedExpiration = subscribeResponse.getExpires();
            authenticateService((BindingProvider) managementPort,"manage");
            GetStatusResponse statusResponse = managementPort.getStatusOp(new GetStatus());
            getSubscriptionDetails();
            ok=(expectedExpiration.equals(statusResponse.getExpires()));
            if(!ok) {
                log.log(Level.SEVERE,"NNM Subscription failure: expectedExpiration("+expectedExpiration+") expiration("+statusResponse.getExpires()+")");
            }
        } catch(Exception e) {
            log.log(Level.SEVERE,"Error renewing subscription: ",e);
        }

        return ok;
    }

   public synchronized void checkNnmSubscription() {
       if(isSubscriptionOk()) {
           log.log(Level.FINEST,"NNM "+eventType+" subscription OK");
       } else {
           try {
               unsubscribe();//unsubscribe to make sure we don't have 2 subscriptions going
           } catch(Exception e) {
               log.log(Level.WARNING,"Failure unsubscribing to NNM "+eventType+": ",e);
           }
           try {
               String results=subscribeWithFilter(filter);//resubscribe
               log.log(Level.CONFIG,"Re-subscribing to NNM "+eventType+": \n"+results);
           } catch(Exception e) {
               log.log(Level.SEVERE,"Failure re-subscribing to NNM "+eventType+": ",e);
           }
       }
   }

    private void authenticateService(BindingProvider bindingProvider,String subscribeManage) {
        String wsdlName=nnmURL+eventContext+"/"+subscribeManage+"?wsdl";
        try {
            Map<String, Object> reqContext = bindingProvider.getRequestContext();
            reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, wsdlName);
            reqContext.put(BindingProvider.USERNAME_PROPERTY, nnmUser);
            reqContext.put(BindingProvider.PASSWORD_PROPERTY, nnmPass);

            updateServiceRequest(bindingProvider);
        } catch(Exception e) {
            log.log(Level.SEVERE,"Error authenticating to ("+wsdlName+"): ",e);
        }
    }

    protected void updateServiceRequest(BindingProvider bindingProvider) {
    }

    private void resetTimers() {
        killTimers();
        enabled=true;
        subscriptionChecker=new SubscriptionChecker();//Periodically check that subscription is still valid.
        subscriptionRenewer=new SubscriptionRenewer();//Periodically renew subscription.
    }

    private void killTimers() {
        enabled=false;
        if(subscriptionChecker!=null) {
            subscriptionChecker.cancel();
            subscriptionChecker=null;
        }
        if(subscriptionRenewer!=null) {
            subscriptionRenewer.cancel();
            subscriptionRenewer=null;
        }
    }

    private SubscribeResponse doSubscribe(String filterString) throws URISyntaxException {
        AddressingProperties reqProps =
            AddressingClientUtil.createDefaultProps(EventingConstants.SUBSCRIBE_ACTION,eventSourceURI);
        reqProps.setMessageID(AddressingClientUtil.createMessageID());
        setRequestProperties((BindingProvider) subscriptionPort,reqProps);

        authenticateService((BindingProvider) subscriptionPort,"subscribe");

        // build subscription payload
        Subscribe request = new Subscribe();

        request.setExpires(new Date(System.currentTimeMillis() + NNM_SUBSCRIBE_TIME));

        // default delivery type with notification EPR
        delivery = getDefaultDelivery();
        request.setDelivery(delivery);

        // receive endTo messages at the same port
        request.setEndTo(delivery.getNotifyTo());

        if (filterString != null) {
            // custom filter that applies to a certain hostname only
            FilterType filter = wrapFilterString(filterString);
            request.setFilter(filter);
        }

        // invoke subscription request
        SubscribeResponse subscriptionTicket = subscriptionPort.subscribeOp(request);

        // check message constraints
        AddressingProperties resProps = getResponseProperties((BindingProvider) subscriptionPort);

        return subscriptionTicket;
    }

    private synchronized String doRenew() {
        String results="";
        AddressingProperties requestProps =
            buildFollowupProperties(subscribeResponse, EventingConstants.RENEW_ACTION, eventSourceURI.toString());
        setRequestProperties((BindingProvider)managementPort, requestProps);

        authenticateService((BindingProvider) managementPort,"manage");
        Renew request = new Renew();
        request.setExpires(new Date(System.currentTimeMillis() + NNM_SUBSCRIBE_TIME));
        RenewResponse renewResponse = managementPort.renewOp(request);
        if(renewResponse!=null && renewResponse.getExpires()!=null) {
            subscribeResponse.setExpires(renewResponse.getExpires());
            results="Renewed until: "+renewResponse.getExpires();
        }
        return results;
    }

    /**
     * Bind request properties.
     */
    private void setRequestProperties(BindingProvider binding,AddressingProperties props) {
        binding.getRequestContext().put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND, props);
    }

    /**
     * Access response properties.
     */
    private AddressingProperties getResponseProperties(BindingProvider binding) {
        return (AddressingProperties) binding.getResponseContext().get(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_INBOUND);
    }

    /**
     * Followup addressing properties basically use the
     * subscription id as wsa:ReferenceParameter
     */
    private AddressingProperties buildFollowupProperties(SubscribeResponse response, String wsaAction, String wsaTo) {
        try {
            AddressingBuilder addrBuilder = AddressingBuilder.getAddressingBuilder();
            AddressingProperties followupProps = addrBuilder.newAddressingProperties();

            //followupProps.setTo(addrBuilder.newURI(wsaTo));
            followupProps.setAction(addrBuilder.newURI(wsaAction));
            followupProps.setMessageID(AddressingClientUtil.createMessageID());

            // necessary in order to parse ref params
            EndpointReference epr = addrBuilder.newEndpointReference(new URI(wsaTo));

            String subscriptionID = response.getSubscriptionId();
            SOAPElementImpl element = new SOAPElementImpl("Identifier", "wse",
                "http://schemas.xmlsoap.org/ws/2004/08/eventing");
            element.setValue(subscriptionID);
            epr.getReferenceParameters().addElement(element);

            followupProps.initializeAsDestination(epr);

            return followupProps;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private DeliveryType getDefaultDelivery() {
        try {
            DeliveryType delivery = new DeliveryType();
            delivery.setMode(EventingConstants.getDeliveryPush().toString());
            EndpointReferenceType notifyEPR = new EndpointReferenceType();
            AttributedURIType attURI = new AttributedURIType();
            attURI.setValue(sinkURL+ejbEP);
            notifyEPR.setAddress(attURI);
            delivery.setNotifyTo(notifyEPR);
            return delivery;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create delivery payload", e);
        }
    }

    private FilterType wrapFilterString(String s) {
        FilterType filter = new FilterType();
        filter.setDialect(EventingConstants.getDialectXPath().toString());
        filter.getContent().add(s);
        return filter;
    }

    class NnmSubscriberTimer {
        protected Timer timer;
        protected long lastRun=0L;
        protected long overInterval=2*60*1000L;
        protected long id;

        public NnmSubscriberTimer(long overInterval) {
            this.overInterval=overInterval;
            lastRun=System.currentTimeMillis();
            id=lastRun;
            timer = new Timer();
        }

        public boolean ok() {
            return System.currentTimeMillis()<lastRun+overInterval;
        }

        public void cancel() {
            if(timer!=null) {
                timer.cancel();
                timer=null;
            }
        }
    }

    class SubscriptionChecker extends NnmSubscriberTimer {

        public SubscriptionChecker() {
            super(2*CHECK_INTERVAL);
            timer.scheduleAtFixedRate(new CheckTask(), CHECK_DELAY, CHECK_INTERVAL);
        }

        class CheckTask extends TimerTask {
            public void run() {
                if(!enabled) {
                    timer.cancel();
                    return;
                }
                try {
                    lastRun=System.currentTimeMillis();
                    checkNnmSubscription();
                } catch(Exception e) {
                    log.log(Level.SEVERE, "Subscription check task failure.");
                }
            }
        }
    }

    class SubscriptionRenewer extends NnmSubscriberTimer {

        public SubscriptionRenewer() {
            super(2*RENEW_DELAY);
            timer.schedule(new RenewTask(), RENEW_DELAY);
        }

        class RenewTask extends TimerTask {
            public void run() {
                if(!enabled) {
                    timer.cancel();
                    return;
                }
                try {
                    lastRun=System.currentTimeMillis();
                    String results=doRenew();
                    log.log(Level.FINER, "Renewing NNM subscription: "+results);
                } catch(Exception e) {
                    log.log(Level.SEVERE, "Renew task failure.");
                } finally {
                    if(timer!=null) {
                        timer.cancel();
                        timer = new Timer();
                        timer.schedule(new RenewTask(), RENEW_DELAY);
                    }
                }
            }
        }
    }
}

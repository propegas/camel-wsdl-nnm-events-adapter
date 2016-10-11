package ru.atc.camel.nnm.events;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.idempotent.FileIdempotentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.atc.adapters.type.Event;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static ru.atc.adapters.message.CamelMessageManager.genHeartbeatMessage;

public class Main {

    private static String activemqPort;
    private static String activemqIp;
    private static String adaptername;
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        logger.info("Starting Custom Apache Camel component example");
        logger.info("Press CTRL+C to terminate the JVM");


        try {
            // get Properties from file
            InputStream input = new FileInputStream("zabbixapi.properties");
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            adaptername = prop.getProperty("adaptername");
            activemqIp = prop.getProperty("activemq.ip");
            activemqPort = prop.getProperty("activemq.port");
        } catch (IOException ex) {
            logger.error("Error while open and parsing properties file", ex);
        }

        logger.info("**** adaptername: " + adaptername);
        logger.info("activemqIp: " + activemqIp);
        logger.info("activemqPort: " + activemqPort);

        org.apache.camel.main.Main main = new org.apache.camel.main.Main();
        main.enableHangupSupport();

        main.addRouteBuilder(new RouteBuilder() {

            @Override
            public void configure() throws Exception {

				/*
                XStream xstream = new XStream();
				xstream.processAnnotations(Event.class);

				XStreamDataFormat xStreamDataFormat = new XStreamDataFormat();
				xStreamDataFormat.setXStream(xstream);
				
				DataFormat myJaxb =
					      new JaxbDataFormat("ru.at_consulting.itsm.event");
				*/
                JsonDataFormat myJson = new JsonDataFormat();
                myJson.setPrettyPrint(true);
                myJson.setLibrary(JsonLibrary.Jackson);
                myJson.setAllowJmsType(true);
                myJson.setUnmarshalType(Event.class);

                PropertiesComponent properties = new PropertiesComponent();
                properties.setLocation("classpath:wsdlnnm.properties");
                getContext().addComponent("properties", properties);

                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                        "tcp://" + activemqIp + ":" + activemqPort);
                getContext().addComponent("activemq", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

                File cachefile = new File("sendedEvents.dat");
                logger.info(String.format("Cache file created: %s", cachefile.createNewFile()));

                from("wsdlnnm://events?"
                        + "delay={{delay}}&"
                        + "wsdlapiurl={{wsdlapiurl}}&"
                        + "wsdlapiport={{wsdlapiport}}&"
                        + "wsusername={{wsusername}}&"
                        + "wspassword={{wspassword}}&"
                        + "wsdlMaxObjects={{wsdlMaxObjects}}&"
                        + "wsdlEventsMaxAgeInDays={{wsdlEventsMaxAgeInDays}}&"
                        + "eventsdump={{eventsdump}}&"
                        + "source={{source}}&"
                        + "adaptername={{adaptername}}")

                        .choice()
                        .when(header("Type").isEqualTo("Error"))
                        .marshal(myJson)
                        .to("activemq:{{eventsqueue}}")
                        .log("Error: ${id} ${header.EventUniqId}")

                        .otherwise()
                        .idempotentConsumer(
                                header("EventIdAndStatus"),
                                FileIdempotentRepository.fileIdempotentRepository(cachefile, 2000, 51200000)
                        )

                        .marshal(myJson)
                        .to("activemq:{{eventsqueue}}")
                        .log("*** NEW EVENT: ${id} ${header.EventIdAndStatus}");

                // Heartbeats
                from("timer://foo?period={{heartbeatsdelay}}")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                genHeartbeatMessage(exchange, adaptername);
                            }
                        })
                        //.bean(WsdlNNMConsumer.class, "genHeartbeatMessage", exchange)
                        .marshal(myJson)
                        .to("activemq:{{heartbeatsqueue}}")
                        .log("*** Heartbeat: ${id}");

            }
        });

        main.run();
    }
}
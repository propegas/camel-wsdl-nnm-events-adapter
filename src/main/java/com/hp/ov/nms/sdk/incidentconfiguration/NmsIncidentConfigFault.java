package com.hp.ov.nms.sdk.incidentconfiguration;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NmsIncidentConfigFault", propOrder = {
    "message"
})
public class NmsIncidentConfigFault implements Serializable {

     protected String message;

        /**
         * Gets the value of the message property.
         *
         * @return
         *     possible object is
         *     {@link String }
         *
         */
        public String getMessage() {
            return message;
        }

        /**
         * Sets the value of the message property.
         *
         * @param value
         *     allowed object is
         *     {@link String }
         *
         */
        public void setMessage(String value) {
            this.message = value;
        }




}

package org.wso2.carbon.custom.tenant.cleanup.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class which contains the attributes of a service response.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ServiceResponse")
public class ServiceResponse implements Serializable {

    private static final long serialVersionUID = 1156043474393339525L;

    @XmlElement(name = "Code")
    private String code;

    @XmlElement(name = "Message")
    private String message;

    /**
     * Get the response code of the operation.
     *
     * @return code
     */
    public String getCode() {

        return code;
    }

    /**
     * Set the response code of the operation.
     *
     * @param code code
     */
    public void setCode(String code) {

        this.code = code;
    }

    /**
     * Get the response message of the operation.
     *
     * @return response message
     */
    public String getMessage() {

        return message;
    }

    /**
     * Set the response message of the operation.
     *
     * @param message Success message
     */
    public void setMessage(String message) {

        this.message = message;
    }
}

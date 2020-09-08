# wso2-tenant-cleanup
An admin service to cleanup a tenant

The existing admin service was that it is taking too much time to delete a tenant. After done some researching further I have concluded that deleting the whole tenant in a single network call is not a good approach. The reason behind that is there is lots of data to be delete. The data need to delete can be varied as for the tenant and its usages. Basically it is not possible to delete a organization and its thousands of users, their access tokens etc. within seconds. Here what we require is that look and feel that the tenant is deleted. Also the deleted tenant should be blocked too. We also should be able to create a tenant with the same domain immediately after delete. So I have check on this internally and come up with the below approach. Here what we do is mark the tenant to deletion. Then delete it later via a database stored procedure scheduled to run at non bisness hours.

1. Deactive the tenant.
2. Then we have to shut down the tenant managers own by that tenant.
3. After that change the tenant name from the table.
4. Now we should be able to create the tenant with the same name.
5. Afterword we can clean up the tenant via a script in the database.

Please find the admin service details from this WSDL file the proect root directory. To invoke this admin service you need to be the super admin user with the below permissions.


To clean up the tenant you have to use the "cleanupTenant" operation with the tenant domain parameter as below. 

```
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://org.apache.axis2/xsd">
   <soapenv:Header/>
   <soapenv:Body>
      <xsd:cleanupTenant>
         <xsd:tenantDomain>test.com</xsd:tenantDomain>
      </xsd:cleanupTenant>
   </soapenv:Body>
</soapenv:Envelope>
```

To add this extension please follow the below steps,

1. Copy the tenant clean up component jar file from here[1] org.wso2.carbon.custom.tenant.cleanup-2.2.0.jar to <IS-HOME>/repository/component/dropins directory.
2. Now start the server and create a test tenant called "test.com" and try to clean up it. If it is successfully cleaned up it should be listed in the tenant list with a similar name as follows "marked_to_delete_test.com_1597922531779". You can check the same from the admin service as well.
3. The successful response of the cleanupTenant call is as follows,
```  
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Body>
      <ns:cleanupTenantResponse xmlns:ns="http://org.apache.axis2/xsd">
         <ns:return xsi:type="ax2100:ServiceResponse" xmlns:ax2100="http://model.cleanup.tenant.custom.carbon.wso2.org/xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <ax2100:code>200</ax2100:code>
            <ax2100:message>TenantDomain test.com cleanup successfully finished.</ax2100:message>
         </ns:return>
      </ns:cleanupTenantResponse>
   </soapenv:Body>
</soapenv:Envelope>
```

Cheers...!

package KPNAutomation;


import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import KPNAutomation.OracleDB;

import static io.restassured.RestAssured.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RedirectConfig;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;


@Listeners({io.qameta.allure.testng.AllureTestNg.class})

public class RegressionsuitePostpaid {
	@BeforeClass
    public void beforeClass() {

        RestAssured.config = RestAssured.config()
                .redirect(RedirectConfig.redirectConfig().followRedirects(true))
                .httpClient(
                    io.restassured.config.HttpClientConfig.httpClientConfig()
                            .setParam("http.connection.timeout", 30000)
                            .setParam("http.socket.timeout", 30000)
                            .setParam("http.connection-manager.timeout", 30000)
                );
    }
	
	 // -----------------------------  Allure helpers  -----------------------------

    private static void attachText(String name, String content) {
        if (content == null) content = "";
        Allure.addAttachment(name, "text/plain", content, StandardCharsets.UTF_8.name());
    }

    /** Attach JSON payload/response to Allure */
    private static void attachJson(String name, String json) {
        if (json == null) json = "";
        Allure.addAttachment(name, "application/json", json, StandardCharsets.UTF_8.name());
    }
    

private static void attachXml(String name, String xml) {
    if (xml == null) xml = "";
    Allure.addAttachment(name, "application/xml", xml, StandardCharsets.UTF_8.name());
}



private static String customerAccountId;
   private static String billingAccountId;
   private static String serviceAccountId;
   private static String multiSaAccountId; 

   private static String msisdn;   // global like prepaid
   private static String iccid;
   private static String imsi;

   private String toPoid(String saId) {
	    String numeric = saId.replaceAll("\\D+", "");  // remove SA prefix
	    return "0.0.0.1+-account+" + numeric;
	}


    private static void attachIdToAllure(String label, String id) {
           Allure.step(label + ": " + id);
           Allure.addAttachment(label, id == null ? "" : id);
       }

    private void sleepSeconds(int s) {
        try { Thread.sleep(s * 1000L); } catch (InterruptedException ignored) {}
    }
    
    
@Epic("Postpaid")
@Feature("Customer Account")
@Story("Create cust Account via API")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 1)
 void CreateCustomerAccount() {
	sleepSeconds(20);
    String payload = "{\n" +
            "   \"extension\": {\n" +
            "       \"accountType\": 1,\n" +
            "       \"accountTag\": \"POSTPAID\",\n" +
            "       \"description\": \"OM\"\n" +
            "   },\n" +
            "   \"businessType\": 1,\n" +
            "   \"locale\": \"en_US\",\n" +
            "   \"contacts\": [\n" +
            "      {\n" +
            "         \"firstName\": \"Customer\",\n" +
            "         \"lastName\": \"Account\",\n" +
            "         \"middleName\": \"\",\n" +
            "         \"salutation\": \"Mr.\",\n" +
            "         \"address\": \"STREET|HOUSE_NUM|HOUSE_NUM_EXT|AP Rotterdam\",\n" +
            "         \"city\": \"Wilhelminakade\",\n" +
            "         \"company\": \"KPN\",\n" +
            "         \"emailAddress\": \"ca@kpn.com\",\n" +
            "         \"state\": \"South Holland\",\n" +
            "         \"zip\": \"3072\",\n" +
            "         \"contactType\": \"Work\",\n" +
            "         \"country\": \"NL\",\n" +
            "         \"phonenumbers\": [\n" +
            "            {\n" +
            "               \"number\": \"78419461\",\n" +
            "               \"phonetype\": 0\n" +
            "            }\n" +
            "         ]\n" +
            "      }\n" +
            "   ]\n" +
            "}";
    
    attachJson("CreateAccount - Request", payload);
   
    ValidatableResponse createRespVR  =
    given()
        .relaxedHTTPSValidation()
        .contentType("application/json")
        .body(payload)
        .log().all()
    .when()
        .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/accounts")
    .then()
        .log().all()
        .statusCode(201);
    
    String createRespBody = createRespVR.extract().asString();
    attachJson("CreateAccount - Response", createRespBody);
    


         customerAccountId = createRespVR.extract().jsonPath().getString("reference.id");
         Assert.assertNotNull(customerAccountId, "CA id is null in response!");
         System.out.println("Created CA id = " + customerAccountId);
         
         attachIdToAllure("Customer Account ID (CA)", customerAccountId);

}



@Epic("Postpaid")
@Feature("Billing Account")
@Story("Create Billing Account via API")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 2)
    public void CreateBillingAccount() {
	sleepSeconds(20);

        Assert.assertNotNull(customerAccountId, "CA id not available from previous step!");

        // Build BA payload with caAccountId = customerAccountId
        String baPayload = "{\n" +
                "  \"extension\": {\n" +
                "    \"accountType\": 2,\n" +
                "    \"accountTag\": \"POSTPAID\",\n" +
                "    \"caAccountId\": \"" + customerAccountId + "\"\n" +
                "  },\n" +
                "  \"contacts\": [\n" +
                "    {\n" +
                "      \"firstName\": \"Billing\",\n" +
                "      \"lastName\": \"Account\",\n" +
                "      \"middleName\": \"\",\n" +
                "      \"salutation\": \"Mr.\",\n" +
                "      \"address\": \"AP Rotterdam\",\n" +
                "      \"city\": \"Wilhelminakade\",\n" +
                "      \"company\": \"KPN\",\n" +
                "      \"emailAddress\": \"ba@kpn.com\",\n" +
                "      \"state\": \"South Holland\",\n" +
                "      \"zip\": \"3072\",\n" +
                "      \"deleted\": false,\n" +
                "      \"newlyCreated\": true,\n" +
                "      \"elem\": 1,\n" +
                "      \"contactType\": \"Work\",\n" +
                "      \"country\": \"NL\",\n" +
                "      \"phonenumbers\": []\n" +
                "    }\n" +
                "  ],\n" +
                "  \"genericBundle\": {},\n" +
                "  \"billUnits\": [\n" +
                "    {\n" +
                "      \"accountingType\": 1,\n" +
                "      \"billingFrequencyInMonths\": 1,\n" +
                "      \"paymentType\": \"10001\",\n" +
                "      \"walletPaymentInstrumentIndex\": 0\n" +
                "    }\n" +
                "  ],\n" +
                "  \"paymentMethod\": [\n" +
                "    {\n" +
                "      \"invoice\": {\n" +
                "        \"id\": \"8097890\",\n" +
                "        \"name\": \"KPN\",\n" +
                "        \"details\": {\n" +
                "          \"invoiceId\": \"8097890\",\n" +
                "          \"deliveryPrefer\": \"1\",\n" +
                "          \"emailAddr\": \"ca@kpn.com\",\n" +
                "          \"name\": \"KPN\",\n" +
                "          \"address\": \"500 Oracle Parkway\",\n" +
                "          \"city\": \"Wilhelminakade\",\n" +
                "          \"state\": \"South Holland\",\n" +
                "          \"zip\": \"94065\",\n" +
                "          \"country\": \"NL\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"paymentType\": \"10001\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        attachJson("CreateAccount - Request", baPayload);
        ValidatableResponse vr =
            given()
                .relaxedHTTPSValidation()
                .contentType("application/json")
                .body(baPayload)
                .log().all()
            .when()
                .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/accounts")
            .then()
                .log().all()
                .statusCode(201);
        
        String createRespBody = vr.extract().asString();
        attachJson("CreateBillingAccount - Response", createRespBody);

        // If the BA create endpoint returns a similar reference structure, capture it:
        billingAccountId = vr.extract().jsonPath().getString("reference.id");
        System.out.println("Created BA id = " + billingAccountId + " (if present in response)");
        
        attachIdToAllure("Billing Account ID (BA)", billingAccountId);
        try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }
@Epic("Postpaid")
@Feature("Service Account")
@Story("Create Service Account via API")
@Severity(SeverityLevel.CRITICAL)
@Test(priority = 3, dependsOnMethods = "CreateBillingAccount")
public void CreateServiceAccount() {
    sleepSeconds(20);

    Assert.assertNotNull(billingAccountId, "BA ID not available from previous step!");

    long epoch = System.currentTimeMillis() / 1000;
    msisdn = "31" + epoch;
    iccid  = "10" + epoch;
    imsi   = "20" + epoch;

    Allure.step("Generated MSISDN : " + msisdn);
    Allure.step("Generated ICCID  : " + iccid);
    Allure.step("Generated IMSI   : " + imsi);

    String payload = "{\n" +
            "   \"extension\": {\n" +
            "       \"accountType\": 3,\n" +
            "       \"accountTag\": \"POSTPAID\",\n" +
            "       \"description\": \"OM\",\n" +
            "       \"baAccountId\": \"" + billingAccountId + "\",\n" +
            "       \"services\": [\n" +
            "           {\n" +
            "               \"aliasList\": [\n" +
            "                   {\"name\": \"MSISDN1\", \"value\": \"" + msisdn + "\"},\n" +
            "                   {\"name\": \"ICCID1\",  \"value\": \"" + iccid  + "\"},\n" +
            "                   {\"name\": \"IMSI1\",   \"value\": \"" + imsi   + "\"}\n" +
            "               ]\n" +
            "           }\n" +
            "       ],\n" +
            "       \"profile\": {\n" +
            "           \"subscriberPreference\": [\n" +
            "               {\"name\": \"NOTIFICATION_CHANNEL\", \"value\": \"SMS\"},\n" +
            "               {\"name\": \"NOTIFICATION_LANGUAGE\", \"value\": \"English\"},\n" +
            "               {\"name\": \"NOTIFICATION_MSISDN\", \"value\": \"45345345\"},\n" +
            "               {\"name\": \"NOTIFICATION_EMAIL\", \"value\": \"test@gmail.com\"},\n" +
            "               {\"name\": \"KPN_PAY\", \"value\": \"Y\"},\n" +
            "               {\"name\": \"BRAND_ID\", \"value\": \"KPN\"},\n" +
            "               {\"name\": \"KPNPAY_CAP\", \"value\": \"70\"},\n" +
            "               {\"name\": \"ROAMING_CAP\", \"value\": \"50\"},\n" +
            "               {\"name\": \"SILENTHOUR_START\", \"value\": \"9AM\"},\n" +
            "               {\"name\": \"SILENTHOUR_END\", \"value\": \"9PM\"}\n" +
            "           ]\n" +
            "       }\n" +
            "   },\n" +
            "   \"locale\": \"en_US\",\n" +
            "   \"businessType\": \"4\",\n" +
            "   \"billUnits\": [\n" +
            "       {\n" +
            "           \"balanceGroups\": [\n" +
            "               {\"extension\": null, \"id\": \"\", \"name\": \"Default Balance Group\", \"elem\": 0}\n" +
            "           ],\n" +
            "           \"walletPaymentInstrumentIndex\": 0\n" +
            "       }\n" +
            "   ],\n" +
            "   \"services\": [\n" +
            "       {\n" +
            "           \"serviceType\": \"/service/telco/gsm\",\n" +
            "           \"serviceKey\": null,\n" +
            "           \"customizedBundles\": [\n" +
            "               {\n" +
            "                   \"customizedChargeOffers\": [\n" +
            "                       {\n" +
            "                           \"name\": \"\",\n" +
            "                           \"description\": \"\",\n" +
            "                           \"baseChargeOfferRef\": {\"id\": \"e45412a4-86d1-4659-812c-7510d6728df4\"},\n" +
            "                           \"purchaseStart\": {\"startDate\": null},\n" +
            "                           \"purchaseEnd\":   {\"endDate\": null},\n" +
            "                           \"overriddenCharges\": [\n" +
            "                               {\"event\": \"7979071291782-8011sdf-dfgrty\"}\n" +
            "                           ]\n" +
            "                       }\n" +
            "                   ]\n" +
            "               }\n" +
            "           ],\n" +
            "           \"subscriptionIndex\": 0\n" +
            "       }\n" +
            "   ]\n" +
            "}";

    attachJson("CreateServiceAccount - Request", payload);

    ValidatableResponse response = ApiRetry.run(
            "CreateServiceAccount",
            () ->
                given()
                    .relaxedHTTPSValidation()
                    .contentType("application/json")
                    .body(payload)
                    .log().all()
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/accounts")
                .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .log().all()
    );

    attachJson("CreateServiceAccount - Response", response.extract().asString());

    serviceAccountId = response.extract().jsonPath().getString("reference.id");
    Assert.assertNotNull(serviceAccountId, "SA ID is null!");

    attachIdToAllure("Service Account ID (old/standard SA)", serviceAccountId);
}


@Epic("Postpaid")
@Feature("Service Account")
@Story("Create Service Account via API")
@Severity(SeverityLevel.CRITICAL)
@Test(priority = 4, dependsOnMethods = "CreateBillingAccount")
public void CreateMultiPostpaidServiceAccount() {

    sleepSeconds(20);
    Assert.assertNotNull(billingAccountId, "BA ID is null!");

    long epoch = System.currentTimeMillis() / 1000;
    String msisdn = "31" + epoch + "7";
    String iccid  = "10" + epoch + "7";
    String imsi   = "20" + epoch + "7";

    String payload =
    "{\n" +
    "  \"extension\": {\n" +
    "    \"accountType\": 3,\n" +
    "    \"accountTag\": \"POSTPAID\",\n" +
    "    \"description\": \"OM\",\n" +
    "    \"baAccountId\": \"" + billingAccountId + "\",\n" +
    "    \"services\": [\n" +
    "      {\n" +
    "        \"aliasList\": [\n" +
    "          {\"name\": \"MSISDN1\", \"value\": \"" + msisdn + "\"},\n" +
    "          {\"name\": \"ICCID1\",  \"value\": \"" + iccid  + "\"},\n" +
    "          {\"name\": \"IMSI1\",   \"value\": \"" + imsi   + "\"}\n" +
    "        ]\n" +
    "      }\n" +
    "    ],\n" +
    "    \"profile\": {\n" +
    "      \"subscriberPreference\": [\n" +
    "        {\"name\": \"NOTIFICATION_CHANNEL\", \"value\": \"SMS\"},\n" +
    "        {\"name\": \"NOTIFICATION_LANGUAGE\", \"value\": \"English\"},\n" +
    "        {\"name\": \"NOTIFICATION_MSISDN\", \"value\": \"45345345\"},\n" +
    "        {\"name\": \"NOTIFICATION_EMAIL\", \"value\": \"test@gmail.com\"},\n" +
    "        {\"name\": \"KPN_PAY\", \"value\": \"Y\"},\n" +
    "        {\"name\": \"BRAND_ID\", \"value\": \"KPN\"},\n" +
    "        {\"name\": \"KPNPAY_CAP\", \"value\": \"70\"},\n" +
    "        {\"name\": \"ROAMING_CAP\", \"value\": \"50\"},\n" +
    "        {\"name\": \"SILENTHOUR_START\", \"value\": \"9AM\"},\n" +
    "        {\"name\": \"SILENTHOUR_END\", \"value\": \"9PM\"}\n" +
    "      ]\n" +
    "    }\n" +
    "  },\n" +
    "  \"locale\": \"en_US\",\n" +
    "  \"businessType\": \"4\",\n" +
    "  \"billUnits\": [\n" +
    "    {\n" +
    "      \"balanceGroups\": [ {\"extension\": null, \"id\": \"\", \"name\": \"Default Balance Group\", \"elem\": 0} ],\n" +
    "      \"walletPaymentInstrumentIndex\": 0\n" +
    "    }\n" +
    "  ],\n" +
    "  \"services\": [\n" +
    "    {\n" +
    "      \"serviceType\": \"/service/telco/gsm\",\n" +
    "      \"serviceKey\": null,\n" +
    "      \"customizedBundles\": [\n" +
    "        {\n" +
    "          \"customizedChargeOffers\": [\n" +
    "            {\n" +
    "              \"name\": \"PrimaryOffering\",\n" +
    "              \"description\": \"\",\n" +
    "              \"quantity\": 1,\n" +
    "              \"status\": 1,\n" +
    "              \"baseChargeOfferRef\": {\"id\": \"315d6ea1-7006-4d6c-823f-54996d38a900\"},\n" +
    "              \"purchaseStart\": {\"startDate\": null},\n" +
    "              \"purchaseEnd\":   {\"endDate\": null},\n" +
    "              \"overriddenCharges\": [ {\"event\": \"2040ba-a02e-11hp-a3jc-sd3w6\"} ]\n" +
    "            },\n" +
    "            {\n" +
    "              \"name\": \"EU_Data_Monthly\",\n" +
    "              \"description\": \"\",\n" +
    "              \"quantity\": 1,\n" +
    "              \"status\": 1,\n" +
    "              \"baseChargeOfferRef\": {\"id\": \"4dcccd4d-67ab-4eec-add2-df5d918155dc\"},\n" +
    "              \"purchaseStart\": {\"startDate\": null},\n" +
    "              \"purchaseEnd\":   {\"endDate\": null},\n" +
    "              \"overriddenCharges\": [ {\"event\": \"1038ba-f22e-p5ew-a1fc-sbqq6\"} ]\n" +
    "            },\n" +
    "            {\n" +
    "              \"name\": \"MB_Transfer\",\n" +
    "              \"description\": \"\",\n" +
    "              \"quantity\": 1,\n" +
    "              \"status\": 1,\n" +
    "              \"baseChargeOfferRef\": {\"id\": \"1d493544-7692-47c5-8a92-654bc93862e4\"},\n" +
    "              \"purchaseStart\": {\"startDate\": null},\n" +
    "              \"purchaseEnd\":   {\"endDate\": null},\n" +
    "              \"overriddenCharges\": [ {\"event\": \"2039ba-f22d-13sj-a1fe-s3dw6\"} ]\n" +
    "            }\n" +
    "          ]\n" +
    "        }\n" +
    "      ],\n" +
    "      \"subscriptionIndex\": 0\n" +
    "    }\n" +
    "  ]\n" +
    "}";

    attachJson("CreateMultiPostpaidServiceAccount - Request", payload);

    ValidatableResponse response =
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .body(payload)
            .log().all()
        .when()
            .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/accounts")
        .then()
            .log().all()
            .statusCode(201);

    multiSaAccountId = response.extract().jsonPath().getString("reference.id");
    attachJson("CreateMultiPostpaidServiceAccount - Response", response.extract().asString());

    Assert.assertNotNull(multiSaAccountId, "Multi-postpaid SA ID is null!");
    attachIdToAllure("Service Account ID (multi-postpaid SA)", multiSaAccountId);
}

@Epic("Postpaid")
@Feature("Balance Transfer")
@Story("MB Transfer between Service Accounts (1 MB)")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 5, dependsOnMethods = {"CreateServiceAccount", "CreateMultiPostpaidServiceAccount"})
public void MBTransfer_Only() {
	sleepSeconds(20);

    Assert.assertNotNull(multiSaAccountId, "Source SA ID is null");
    Assert.assertNotNull(serviceAccountId, "Target SA ID is null");

    String payload = "{\n" +
            "  \"operation\": \"UPDATE\",\n" +
            "  \"searchCriteria\": {\n" +
            "    \"entityType\": \"ServiceAccount\",\n" +
            "    \"filters\": {\"accountId\": \"" + multiSaAccountId + "\"}\n" +
            "  },\n" +
            "  \"relatedEntities\": [\n" +
            "    {\"entityType\": \"ServiceAccount\", \"filters\": {\"accountId\": \"" + serviceAccountId + "\"}, \"role\": \"target\"}\n" +
            "  ],\n" +
            "  \"transferDetail\": {\"amount\": {\"amount\": 1, \"units\": \"MB\"}}\n" +
            "}";

    attachJson("MBTransfer - Request", payload);

    ValidatableResponse vr = ApiRetry.run(
            "MBTransfer",
            () ->
                given()
                    .relaxedHTTPSValidation()
                    .contentType("application/json")
                    .body(payload)
                    .log().all()
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/transferBalance")
                .then()
                    .log().all()
                    .statusCode(200)
    );

    attachJson("MBTransfer - Response", vr.extract().asString());
}

@Epic("Postpaid")
@Feature("Balance Transfer")
@Story("Get MB Transfer history (Target SA)")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 6, dependsOnMethods = {"CreateServiceAccount", "MBTransfer_Only"})
public void GetTransferredBalance_MB() {

    sleepSeconds(20);
    Assert.assertNotNull(serviceAccountId, "Service Account ID is null!");

    // Convert SA → POID 
    String poid = toPoid(serviceAccountId);

    
    String url =
            "https://st.dbss-rm.oci-np.kpn.org/brm/prepayBalanceManagement/v4/transferBalance"
            + "?receiverPartyAccount.id=" + poid
            + "&limit=10";
    attachText("GetTransferredBalance_MB - URL", url);

    ValidatableResponse response = ApiRetry.run(
            "GetTransferredBalance_MB",
            () ->
                given()
                    .relaxedHTTPSValidation()
                    .log().all()
                .when()
                    .get(url)
                .then()
                    .log().all()
                    .statusCode(200)
    );

    attachJson("GetTransferredBalance_MB - Response", response.extract().asString());
}




@Epic("Postpaid")
@Feature("Subscriber List")
@Story("Get Subscriber List using Billing Account ID")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 7, dependsOnMethods = "CreateBillingAccount")
public void GetSubscriberList_UsingBillingAccountID() {
	sleepSeconds(20);

	Assert.assertNotNull(billingAccountId, "Billing Account POID is null; run CreateBillingAccount first!");

    String payload =
            "{\n" +
            "  \"operation\": \"SEARCH\",\n" +
            "  \"searchCriteria\": {\n" +
            "    \"entityType\": \"BillingAccount\",\n" +
            "    \"filters\": {\n" +
            "      \"accountId\": \"" + billingAccountId + "\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

    attachJson("GetSubscriberList - Request", payload);

    ValidatableResponse response = ApiRetry.run(
            "GetSubscriberList",
            () ->
                given()
                    .relaxedHTTPSValidation()
                    .contentType("application/json")
                    .body(payload)
                    .log().all()
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getSubscriberList")
                .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .log().all()
    );

    attachJson("GetSubscriberList - Response", response.extract().asString());

    System.out.println("GetSubscriberList executed for BA: " + billingAccountId);
}

@Epic("Postpaid")
@Feature("Payment Method Change")
@Story("Change Payment Method from Invoice to SEPA")
@Severity(SeverityLevel.CRITICAL)
@Test(priority = 8, dependsOnMethods = "CreateBillingAccount")
public void ChangePaymentMethod_To_SEPA() {
	sleepSeconds(20);

    Assert.assertNotNull(billingAccountId, "Billing Account POID is null!");

    // Generate unique SEPA mandate reference like MSISDN style
    String mandateUniqueReference = "MREF" + System.currentTimeMillis();

    // Generate current timestamp in ISO-8601 format
    String mandateSignedT = java.time.LocalDateTime.now().toString();

    String payload = "{\n" +
            "  \"operation\": \"UPDATE\",\n" +
            "  \"searchCriteria\": {\n" +
            "    \"entityType\": \"BillingAccount\",\n" +
            "    \"filters\": {\n" +
            "      \"accountId\": \"" + billingAccountId + "\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"sepaInfoDetail\": {\n" +
            "    \"mandateType\": 1,\n" +
            "    \"iban\": \"DE44500105175407324931\",\n" +
            "    \"bic\": \"DEUTDEFF500\",\n" +
            "    \"mandateUniqueReference\": \"" + mandateUniqueReference + "\",\n" +
            "    \"mandateSignedT\": \"" + mandateSignedT + "\",\n" +
            "    \"debtorInfo\": {\n" +
            "      \"contacts\": {\n" +
            "        \"firstName\": \"Billing\",\n" +
            "        \"address\": \"AP Rotterdam\",\n" +
            "        \"city\": \"Wilhelminakade\",\n" +
            "        \"state\": \"SOUTH HOLLAND\",\n" +
            "        \"zip\": \"3072\",\n" +
            "        \"country\": \"NL\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"creditorInfo\": {\n" +
            "      \"creditorId\": \"NL12ZZZ123456789000\",\n" +
            "      \"contacts\": {\n" +
            "        \"firstName\": \"NL_Creditor\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    attachJson("ChangePaymentMethod-SEPA - Request", payload);

    ValidatableResponse response = ApiRetry.run(
            "ChangePaymentMethod-SEPA",
            () ->
                given()
                    .relaxedHTTPSValidation()
                    .contentType("application/json")
                    .body(payload)
                    .log().all()
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/changePaymentMethod/sepa")
                .then()
                    .log().all()
                    .statusCode(200)
    );

    attachJson("ChangePaymentMethod-SEPA - Response", response.extract().asString());
}

@Epic("Postpaid")
@Feature("Change Offering")
@Story("Add Offering to Service Account via API")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 9, dependsOnMethods = "CreateServiceAccount")
public void ChangeOfferingADD() {
	sleepSeconds(20);
    Assert.assertNotNull(serviceAccountId, "Service Account id not available!");
    Assert.assertNotNull(msisdn, "MSISDN not available!");
    Instant now = Instant.now();
    String startDate = DateTimeFormatter.ISO_INSTANT.format(now);
    String endDate   = DateTimeFormatter.ISO_INSTANT.format(now.plus(31, ChronoUnit.DAYS));
    String AddOfferingPayload = "{\r\n" +
        "  \"operation\": \"ADD\",\r\n" +
        "  \"searchCriteria\": {\r\n" +
        "    \"entityType\": \"ServiceAccount\",\r\n" +
        "    \"filters\": {\r\n" +
        "      \"accountId\": \"" + serviceAccountId + "\"\r\n" +
        "    }\r\n" +
        "  },\r\n" +
        "  \"product\": [\r\n" +
        "    {\r\n" +
        "      \"actionType\": \"ADD\",\r\n" +
        "      \"services\": [\r\n" +
        "        {\r\n" +
        "          \"serviceType\": \"/service/telco/gsm\",\r\n" +
        "          \"customizedBundles\": [\r\n" +
        "            {\r\n" +
        "              \"customizedChargeOffers\": [\r\n" +
        "                {\r\n" +
        "                  \"name\": \"\",\r\n" +
        "                  \"description\": \"\",\r\n" +
        "                  \"quantity\": 1,\r\n" +
        "                  \"status\": 1,\r\n" +
        "                  \"baseChargeOfferRef\": {\r\n" +
        "                    \"id\": \"e03156d1-fc37-45e0-985d-c0aa223c853b\"\r\n" +
        "                  },\r\n" +
        "                  \"purchaseStart\": {\r\n" +
        "                    \"startDate\": \"" + startDate + "\"\r\n" +
        "                  },\r\n" +
        "                  \"purchaseEnd\": {\r\n" +
        "                    \"endDate\": \"" + endDate + "\"\r\n" +
        "                  },\r\n" +
        "                  \"overriddenCharges\": [\r\n" +
        "                    {\r\n" +
        "                      \"event\": \"12650937gadsf245362345234\"\r\n" +
        "                    }\r\n" +
        "                  ]\r\n" +
        "                }\r\n" +
        "              ]\r\n" +
        "            }\r\n" +
        "          ],\r\n" +
        "          \"subscriptionIndex\": 0\r\n" +
        "        }\r\n" +
        "      ]\r\n" +
        "    }\r\n" +
        "  ]\r\n" +
        "}";
    attachJson("ChangeOfferingADD - Request", AddOfferingPayload);
    ValidatableResponse changeRespVR =
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .body(AddOfferingPayload)
            .log().all()
        .when()
            .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/changeOffering")
        .then()
            .log().all();
    int status = changeRespVR.extract().statusCode();
    String changeRespBody = changeRespVR.extract().asString();
    attachJson("ChangeOfferingADD - Response", changeRespBody);
    if (status == 200 || status == 201) {
        System.out.println("ChangeOffering ADD completed for ServiceAccount: " + serviceAccountId +
                " | MSISDN : " + msisdn +
                " | startDate: " + startDate +
                " | endDate: " + endDate);
        Allure.step("ChangeOffering ADD completed for ServiceAccount " + serviceAccountId);
    } else {
        String errorDesc = changeRespVR.extract().jsonPath().getString("statusDescription");
        Assert.fail("Change Offering ADD failed: " + errorDesc);
    }
}


@Epic("Postpaid")
@Feature("Change Offering")
@Story("Remove Offering from Service Account via API")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 10, dependsOnMethods = "CreateServiceAccount")
public void ChangeOfferingRemove() {
	sleepSeconds(20);
    Assert.assertNotNull(serviceAccountId, "Service Account id not available!");
    Assert.assertNotNull(msisdn, "MSISDN not available!");
    String RemoveOfferingPayload = "{\r\n" +
        "  \"operation\": \"REMOVE\",\r\n" +
        "  \"searchCriteria\": {\r\n" +
        "    \"entityType\": \"ServiceAccount\",\r\n" +
        "    \"filters\": {\r\n" +
        "      \"accountId\": \"" + serviceAccountId + "\"\r\n" +
        "    }\r\n" +
        "  },\r\n" +
        "  \"product\": [\r\n" +
        "    {\r\n" +
        "      \"actionType\": \"REMOVE\",\r\n" +
        "      \"services\": [\r\n" +
        "        {\r\n" +
        "          \"serviceType\": \"/service/telco/gsm\",\r\n" +
        "          \"serviceKey\": \"e03156d1-fc37-45e0-985d-c0aa223c853b\",\r\n" +
        "          \"customizedBundles\": [\r\n" +
        "            {\r\n" +
        "              \"customizedChargeOffers\": [\r\n" +
        "                {\r\n" +
        "                  \"quantity\": 1,\r\n" +
        "                  \"overriddenCharges\": [\r\n" +
        "                    {\r\n" +
        "                      \"event\": \"12650937gadsf245362345234\"\r\n" +
        "                    }\r\n" +
        "                  ]\r\n" +
        "                }\r\n" +
        "              ]\r\n" +
        "            }\r\n" +
        "          ],\r\n" +
        "          \"subscriptionIndex\": 0\r\n" +
        "        }\r\n" +
        "      ]\r\n" +
        "    }\r\n" +
        "  ]\r\n" +
        "}";
    attachJson("ChangeOfferingRemove - Request", RemoveOfferingPayload);
    ValidatableResponse changeRespVR =
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .body(RemoveOfferingPayload)
            .log().all()
        .when()
            .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/changeOffering")
        .then()
            .log().all();
    int status = changeRespVR.extract().statusCode();
    String changeRespBody = changeRespVR.extract().asString();
    attachJson("ChangeOfferingRemove - Response", changeRespBody);
    if (status == 200 || status == 201) {
        System.out.println("ChangeOffering REMOVE completed for ServiceAccount: " + serviceAccountId +
                " | MSISDN : " + msisdn);
        Allure.step("ChangeOffering REMOVE completed for ServiceAccount " + serviceAccountId);
    } else {
        String errorDesc = changeRespVR.extract().jsonPath().getString("statusDescription");
        Assert.fail("Change Offering REMOVE failed: " + errorDesc);
    }
}

@Epic("Postpaid")
@Feature("Bundle Balance")
@Story("Get Bundle Balance")
@Severity(SeverityLevel.NORMAL)
@Test(dependsOnMethods = "CreateServiceAccount", priority = 11)
public void GetBundleBalance() {
	
	sleepSeconds(20);
    Assert.assertNotNull(msisdn, "MSISDN not available from Service Account creation test");
    String payload =
        "{\r\n" +
        "  \"operation\": \"SEARCH\",\r\n" +
        "  \"searchCriteria\": {\r\n" +
        "    \"entityType\": \"ServiceAccount\",\r\n" +
        "    \"filters\": {\r\n" +
        "      \"aliasList\": [\r\n" +
        "        {\r\n" +
        "          \"name\": \"MSISDN1\",\r\n" +
        "          \"value\": \"" + msisdn + "\"\r\n" +
        "        }\r\n" +
        "      ]\r\n" +
        "    }\r\n" +
        "  }\r\n" +
        "}";
    attachJson("GetBundleBalance - Request", payload);
    Allure.step("Fetching bundle balance for MSISDN=" + msisdn);
    RequestSpecification spec = new RequestSpecBuilder()
            .setRelaxedHTTPSValidation()
            .setContentType("application/json")
            .addHeader("Accept", "application/json")
            .addHeader("Connection", "keep-alive")
            .build();
    RestAssured.config = RestAssured.config()
            .redirect(RedirectConfig.redirectConfig().followRedirects(true));
    ValidatableResponse vr =
        given()
            .spec(spec)
            .body(payload)
            .log().all()
        .when()
            .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getBundleBalance")
        .then()
            .statusCode(200)
            .log().all();
    String respBody = vr.extract().asString();
    attachJson("GetBundleBalance - Response", respBody);
    Allure.step("Bundle Balance fetched successfully for MSISDN=" + msisdn);
}
@Epic("Postpaid")
@Feature("Billing Cycle")
@Story("Get Billing Date of Month")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 12, dependsOnMethods = "CreateBillingAccount")
public void getBDOM() {
	sleepSeconds(20);
    Assert.assertNotNull(billingAccountId, "Billing Account ID not available from previous step!");
    // Construct the URL dynamically with the Billing Account ID
    String url = "https://st.dbss-rm.oci-np.kpn.org/brm/accountManagement/v4/billingCycleSpecification/"
                 + billingAccountId + "?fields=billingDateShift";
    Allure.step("Fetching billingDateShift for Billing Account ID=" + billingAccountId);
    // Relax SSL validation to bypass certificate trust issues in test environments
    ValidatableResponse vr =
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .log().all()
        .when()
            .get(url)
        .then()
            .statusCode(200)
            .log().all();
    // Extract full response
    String respBody = vr.extract().asString();
    attachJson("GetBDOM - Response", respBody);
    // Extract the billingDateShift field directly
    String billingDateShift = vr.extract().jsonPath().getString("billingDateShift");
    System.out.println("Billing Date of Month (BDOM) for BA " + billingAccountId + " = " + billingDateShift);
    Allure.step("Billing Date of Month (BDOM) fetched successfully: " + billingDateShift);
}
@Epic("Postpaid")
@Feature("Billing Cycle")
@Story("Update Billing Date of Month")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 13, dependsOnMethods = "CreateBillingAccount")
public void UpdateBDOM() {
	sleepSeconds(20);
    Assert.assertNotNull(billingAccountId, "Billing Account ID not available from previous step!");
    // Step 1: Check current value
    String currentShift =
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
        .when()
            .get("https://st.dbss-rm.oci-np.kpn.org/brm/accountManagement/v4/billingCycleSpecification/"
                 + billingAccountId + "?fields=billingDateShift")
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("billingDateShift");
    Allure.step("Current Billing Date Shift for BA " + billingAccountId + " = " + currentShift);
    // Step 2: Send update event
    String payload = "{ \"eventType\": \"PartyAccountAttributeValueChangeEvent\", \"event\": { \"partyAccount\": { \"id\": \"" 
                     + billingAccountId + "\", \"billStructure\": { \"cycleSpecification\": { \"billingDateShift\": \"15\" } } } } }";
    ValidatableResponse updateRespVR =
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .body(payload)
        .when()
            .post("https://st.dbss-rm.oci-np.kpn.org/brm/accountManagement/v4/listener/partyAccountAttributeValueChangeEvent")
        .then()
            .statusCode(201)
            .log().all();
    
    attachJson("Update BDOM - Request", payload);
    
    // Capture and attach the response body (may be empty)
    String updateRespBody = updateRespVR.extract().asString();
    if (updateRespBody == null || updateRespBody.isEmpty()) {
        updateRespBody = "{}"; // represent empty response
    }
    attachJson("UpdateBDOM - Response", updateRespBody);
    Allure.step("UpdateBDOM event sent for BA " + billingAccountId);
    // Step 3: Verify new value
    String newShift =
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
        .when()
            .get("https://st.dbss-rm.oci-np.kpn.org/brm/accountManagement/v4/billingCycleSpecification/" + billingAccountId + "?fields=billingDateShift")
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("billingDateShift");
    Allure.step("Billing Date Shift fetched successfully for BA " + billingAccountId + ": " + newShift);
    Assert.assertEquals(newShift, "15", "Billing Date Shift should be updated to 15");
}


@Epic("Postpaid")
@Feature("Customer Account")
@Story("Update Customer Account Profile via API")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 14, dependsOnMethods = "CreateCustomerAccount")

public void UpdateCustomerAccountProfile() {

	sleepSeconds(20);

    Assert.assertNotNull(customerAccountId, "Customer Account ID not available!");
    String payload = "{\n" +
            "  \"operation\": \"UPDATE\",\n" +
            "  \"searchCriteria\": {\n" +
            "    \"entityType\": \"CustomerAccount\",\n" +
            "    \"filters\": {\n" +
            "      \"accountId\": \"" + customerAccountId + "\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"partyAccount\": {\n" +
            "    \"@type\": \"CustomerAccount\",\n" +
            "    \"krnId\": \"80978901122\"\n" +
            "  }\n" +
            "}";
    attachJson("UpdateProfile - Request", payload);

    ValidatableResponse updateRespVR =
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .body(payload)
            .log().all()
        .when()
            .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/updateProfile")
        .then()
            .log().all()
            .statusCode(200);

    String updateRespBody = updateRespVR.extract().asString();
    attachJson("UpdateProfile - Response", updateRespBody);

    Allure.step("UpdateProfile completed for Customer Account ID: " + customerAccountId);
    System.out.println("Updated profile for Customer Account ID = " + customerAccountId);

}

@Epic("Postpaid")
@Feature("Account Hierarchy")
@Story("Get Account Hierarchy by MSISDN (Postpaid)")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 15, dependsOnMethods = {"CreateServiceAccount"})
public void GetAccountHierarchy_MSISDN() {
 
    sleepSeconds(20);
 
    Assert.assertNotNull(msisdn, "MSISDN not available from CreateServiceAccount test");
    final String msisdnToQuery = msisdn;
 
    final String url = "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getAccountHierarchy";
 
    final String payload =
        "{\n" +
        "  \"operation\": \"SEARCH\",\n" +
        "  \"searchCriteria\": {\n" +
        "    \"entityType\": \"ServiceAccount\",\n" +
        "    \"filters\": {\n" +
        "      \"aliasList\": [\n" +
        "        { \"name\": \"MSISDN1\", \"value\": \"" + msisdnToQuery + "\" }\n" +
        "      ]\n" +
        "    }\n" +
        "  }\n" +
        "}";
 
    attachJson("GetAccountHierarchy (Postpaid, MSISDN) - Request", payload);
    Allure.step("Fetching Account Hierarchy using MSISDN=" + msisdnToQuery);
 
    ValidatableResponse vr = ApiRetry.run(
        "GetAccountHierarchy_MSISDN_Postpaid",
        () ->
            given()
                .relaxedHTTPSValidation()
                .contentType("application/json")
                .header("Accept", "application/json")
                .body(payload)
                .log().all()
            .when()
                .post(url)
            .then()
                .log().all()
                .statusCode(200)
    );
 
    final String respBody = vr.extract().asString();
    attachJson("GetAccountHierarchy (Postpaid, MSISDN) - Response", respBody);
    Allure.step("Account Hierarchy fetched successfully for MSISDN=" + msisdnToQuery);
}
 
@Epic("Postpaid")
@Feature("Payment Method")
@Story("Get Payment Method using Billing Account ID")
@Severity(SeverityLevel.NORMAL)
@Test(priority = 16, dependsOnMethods = "CreateBillingAccount")
public void GetPaymentMethod() {
 
    sleepSeconds(20);
    Assert.assertNotNull(billingAccountId, "Billing Account ID is null!");
 
    
    final String url = 
        "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getPaymentMethod";
 
    
    final String payload =
        "{\n" +
        "  \"operation\": \"SEARCH\",\n" +
        "  \"searchCriteria\": {\n" +
        "    \"entityType\": \"BillingAccount\",\n" +
        "    \"filters\": {\n" +
        "      \"accountId\": \"" + billingAccountId + "\"\n" +
        "    }\n" +
        "  }\n" +
        "}";
 
    attachJson("GetPaymentMethod - Request", payload);
    Allure.step("Fetching Payment Method for BillingAccountId=" + billingAccountId);
 
    ValidatableResponse vr = ApiRetry.run(
        "GetPaymentMethod",
        () ->
            given()
                .relaxedHTTPSValidation()
                .contentType("application/json")
                .header("Accept", "application/json")
                .body(payload)
                .log().all()
            .when()
                .post(url)
            .then()
                .log().all()
                .statusCode(200)
    );
 
    String responseBody = vr.extract().asString();
    attachJson("GetPaymentMethod - Response", responseBody);
 
    Allure.step("Payment Method fetched successfully for BillingAccountId=" + billingAccountId);
    System.out.println("Payment Method fetched for BA = " + billingAccountId);
}



}


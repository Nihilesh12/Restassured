package KPNAutomation;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;



import static io.restassured.RestAssured.*;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
public class RegressionsuitePrepaid {

    // declaring globally
    private static String MSISDN;   
    private static String serviceAccountId; 
    private static String billingAccountId;
    private static String customerAccountId;
    private static Long lastAccountObjId0; // NEW: numeric account_obj_id0 from CreateAccount
    
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


    /** Write Allure environment once per run  */
    private static void writeAllureEnvironment() {
        try {
            Path dir = Paths.get("target", "allure-results");
            Files.createDirectories(dir);
            Properties p = new Properties();
            p.setProperty("Environment", "ST");
            p.setProperty("SIM preactivation Base URL", "http://localhost:1119");
            p.setProperty("Topup URL", "https://st.dbss-rm.oci-np.kpn.org/brm/extn/prepayBalanceManagement/v4/topupBalance");
            p.setProperty("AddOffering URL", "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/changeOffering");
            p.setProperty("ChangeServiceAttribute URL", "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/changeServiceAttribute");
            p.setProperty("Database", "Oracle (ojdbc8)");
            try (OutputStream os = Files.newOutputStream(dir.resolve("environment.properties"))) {
                p.store(os, "Allure environment");
            }
        } catch (IOException ignore) {
            // no-op
        }
    }
    
    

private void attachDbBalance(String name, Long balance) {
    String value = (balance == null) ? "NULL" : balance.toString();
    Allure.addAttachment(
            name,
            "text/plain",
            "Current Balance = " + value
    );
}





          
    // ------------------------------------------------------------------------------------------------

    @Epic("Prepaid")
    @Feature("SIM PreActivation")
    @Story("Create Account via API")
    @Severity(SeverityLevel.NORMAL)
    @Test(priority = 1)
    void CreateAccount() {

        // ---------------- DB Connection Data ----------------
        String jdbcUrl = "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com";
        String dbUser = "pin";
        String dbPass = "BrmDb5#rm#11";

        OracleDB db = new OracleDB(jdbcUrl, dbUser, dbPass);

        String epochSql = "SELECT trunc((CAST(SYSTIMESTAMP AT TIME ZONE 'UTC' AS DATE) - DATE '1970-01-01') * 86400) FROM dual";
        Long epochValue = db.getSingleLong(epochSql);

        String msisdn = String.valueOf(epochValue);

        // store for TopUp 
        MSISDN = "31" + msisdn;

        // Record environment (once per run)
        writeAllureEnvironment();
        Allure.step("Generated MSISDN from Current Timestamp: " + msisdn);
        Allure.step("Updated MSISDN : " + MSISDN);
        
        Instant now = Instant.now();
        String startDate = DateTimeFormatter.ISO_INSTANT.format(now);

        String Payload  = "{\r\n"
                + "\"locale\": \"en_US\",\r\n"
                + "\"businessType\": \"3\",\r\n"
                + "\"services\": [\r\n"
                + "  {\r\n"
                + "   \"serviceType\": \"/service/telco/gsm\",\r\n"
                + "   \"serviceKey\": {\r\n"
                + "    \"string\": \"service-id"+ msisdn +"\"\r\n"
                + "   },\r\n"
                + "   \"customizedBundles\": [\r\n"
                + "    {\r\n"
                + "     \"customizedChargeOffers\": [\r\n"
                + "      {\r\n"
                + "       \"name\": \"KPN Prepaid\",\r\n"
                + "       \"description\": \"\",\r\n"
                + "       \"quantity\": 1,\r\n"
                + "       \"status\": 1,\r\n"
                + "       \"baseChargeOfferRef\": {\r\n"
                + "        \"id\": \"kpn_prepaid_001\"\r\n"
                + "       },\r\n"
                + "       \"purchaseStart\": {\r\n"
                + "        \"startDate\": {\r\n"
                + "         \"string\": \"2026-03-16T10:43:32.914705300+01:00\"\r\n"
                + "        }\r\n"
                + "       },\r\n"
                + "       \"purchaseEnd\": {\r\n"
                + "        \"endDate\": {\r\n"
                + "         \"string\": \"2027-03-16T10:43:32.914949458+01:00\"\r\n"
                + "        }\r\n"
                + "       },\r\n"
                + "       \"overriddenCharges\": [\r\n"
                + "        {\r\n"
                + "         \"event\": \"KP1773654208844803-0001\"\r\n"
                + "        }\r\n"
                + "       ]\r\n"
                + "      },\r\n"
                + "      {\r\n"
                + "       \"name\": \"KPN Prepaid Initial Balance\",\r\n"
                + "       \"description\": \"\",\r\n"
                + "       \"quantity\": 1,\r\n"
                + "       \"status\": 1,\r\n"
                + "       \"baseChargeOfferRef\": {\r\n"
                + "        \"id\": \"kpn_prepaid_initial_balance_5_001\"\r\n"
                + "       },\r\n"
                + "       \"purchaseStart\": {\r\n"
                + "        \"startDate\": {\r\n"
                + "         \"string\": \"2026-03-16T10:43:32.914705300+01:00\"\r\n"
                + "        }\r\n"
                + "       },\r\n"
                + "       \"purchaseEnd\": {\r\n"
                + "        \"endDate\": {\r\n"
                + "         \"string\": \"2027-03-16T10:43:32.914949458+01:00\"\r\n"
                + "        }\r\n"
                + "       },\r\n"
                + "       \"overriddenCharges\": [\r\n"
                + "        {\r\n"
                + "         \"event\": \"KP177364654208844803-0001\"\r\n"
                + "        }\r\n"
                + "       ]\r\n"
                + "      },\r\n"
                + "      {\r\n"
                + "       \"name\": \"KPN Prepaid tarif\",\r\n"
                + "       \"description\": \"\",\r\n"
                + "       \"quantity\": 1,\r\n"
                + "       \"status\": 1,\r\n"
                + "       \"baseChargeOfferRef\": {\r\n"
                + "        \"id\": \"kpn_prepaid_basis_2021_001\"\r\n"
                + "       },\r\n"
                + "       \"purchaseStart\": {\r\n"
                + "        \"startDate\": {\r\n"
                + "         \"string\": \"2026-03-16T10:43:32.914705300+01:00\"\r\n"
                + "        }\r\n"
                + "       },\r\n"
                + "       \"purchaseEnd\": {\r\n"
                + "        \"endDate\": {\r\n"
                + "         \"string\": \"2027-03-16T10:43:32.914949458+01:00\"\r\n"
                + "        }\r\n"
                + "       },\r\n"
                + "       \"overriddenCharges\": [\r\n"
                + "        {\r\n"
                + "         \"event\": \"KP1773654208856444803-0001\"\r\n"
                + "        }\r\n"
                + "       ]\r\n"
                + "      }\r\n"
                + "     ]\r\n"
                + "    }\r\n"
                + "   ]\r\n"
                + "  }\r\n"
                + "],\r\n"
                + "\"extension\": {\r\n"
                + "  \"caAccountId\": null,\r\n"
                + "  \"baAccountId\": null,\r\n"
                + "  \"saAccountId\": null,\r\n"
                + "  \"accountType\": \"3\",\r\n"
                + "  \"accountTag\": \"Prepaid\",\r\n"
                + "  \"description\": \"PREACTIVATION_EVENT\",\r\n"
                + "  \"kpnBrandId\": \"201\",\r\n"
                + "  \"kpnTaxExempt\": null,\r\n"
                + "  \"kpnTaxRevers\": null,\r\n"
                + "  \"kpnTaxNo\": null,\r\n"
                + "  \"services\": [\r\n"
                + "   {\r\n"
                + "    \"serviceKey\": null,\r\n"
                + "    \"aliasList\": [\r\n"
                + "     {\r\n"
                + "      \"name\": \"MSISDN1\",\r\n"
                + "      \"value\": \"31" + msisdn + "\"\r\n"
                + "     },\r\n"
                + "     {\r\n"
                + "      \"name\": \"ICCID1\",\r\n"
                + "      \"value\": \"10" + msisdn + "\"\r\n"
                + "     },\r\n"
                + "     {\r\n"
                + "      \"name\": \"IMSI1\",\r\n"
                + "      \"value\": \"20" + msisdn + "\"\r\n"
                + "     }\r\n"
                + "    ]\r\n"
                + "   }\r\n"
                + "  ],\r\n"
                + "  \"profile\": {\r\n"
                + "   \"subscriberPreference\": [\r\n"
                + "    {\r\n"
                + "     \"name\": \"EARLIEST_ACTIVATION_TIME\",\r\n"
                + "     \"value\": \"2026-01-01T00:00:00\",\r\n"
                + "     \"subscriberPreferenceId\": null\r\n"
                + "    },\r\n"
                + "    {\r\n"
                + "     \"name\": \"LATEST_ACTIVATION_TIME\",\r\n"
                + "     \"value\": \"2026-01-01T00:00:00\",\r\n"
                + "     \"subscriberPreferenceId\": null\r\n"
                + "    }\r\n"
                + "   ]\r\n"
                + "  }\r\n"
                + "}\r\n"
                + "}";

        // --- API Call ---
        attachJson("CreateAccount - Request", Payload);

        ValidatableResponse createRespVR =
            given()
                .relaxedHTTPSValidation()
                .contentType("application/json")
                .header("Accept", "*/*")
                .header("kpn.event.schema.name", "SimPreActivationV1")
                .header("kpn.source.system.id", "whole-seller-id")
                .header("kpn.source.interaction.id", "")
                .header("User-Agent", "PostmanRuntime/7.43.0")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("kpn.event.id", "000000021")
                .header("kpn-event-type", "dbssrm.sim_preactivation_request")
                .header("schema-id", "dbssrm/sim_preactivation_request/v0.0.1/schema.avsc")
                .header("kpn-specversion", "2.0")
                .body(Payload)
                .log().all()
            .when()
                .post("http://localhost:1119/api/events")
            .then()
                .log().ifValidationFails()
               .statusCode(200)
                .body(org.hamcrest.Matchers.containsString("Event sent successfully to Kafka"))
                .log().all();

        String createRespBody = createRespVR.extract().asString();
        attachJson("CreateAccount - Response", createRespBody);
        Allure.step("Preactivation event posted and acknowledged by gateway");

        // ----------------  MSISDN -> account_obj_id0 ----------------
        String sqlAccount =
                "select s.account_obj_id0 " +
                "from service_t s " +
                "join service_alias_list_t st on s.poid_id0 = st.obj_id0 " +
                "where st.name = ?";

        String aliasValue = "31" + msisdn;

        Long accountObjId0 = null;
        long deadline = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < deadline) {
            accountObjId0 = db.getSingleLong(sqlAccount, aliasValue);
            if (accountObjId0 != null) break;
            try { Thread.sleep(3000); } catch (InterruptedException ie) {}
        }

        Assert.assertNotNull(accountObjId0, "account_obj_id0 not found in DB for MSISDN: " + aliasValue);
        System.out.println("Fetching Service account_obj_id0 from DB  = " + accountObjId0);
        Allure.step("Fetched service account_obj_id0 from DB= " + accountObjId0 + " for MSISDN " + aliasValue);

        // NEW: store numeric id for later test usage
        lastAccountObjId0 = accountObjId0;  // <--- NEW

        String sqlAccounts =
                "SELECT " +
                        "  'Customer Account: CA' || REGEXP_SUBSTR(lineage, '\\/0\\.0\\.0\\.1:([0-9]+)', 1, 1, NULL, 1) AS CA, " +
                        "  'Billing_account: BA'   || REGEXP_SUBSTR(lineage, '\\/0\\.0\\.0\\.1:([0-9]+)', 1, 2, NULL, 1) AS BA, " +
                        "  'Service_account: SA'   || REGEXP_SUBSTR(lineage, '\\/0\\.0\\.0\\.1:([0-9]+)', 1, 3, NULL, 1) AS SA " +
                        "FROM account_t WHERE poid_id0 = ?";

        List<Map<String, Object>> rows = db.getRows(sqlAccounts, accountObjId0);
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            System.out.println(r.get("CA"));
            System.out.println(r.get("BA"));
            System.out.println(r.get("SA"));

            // Capture service account ID
            String saStr = String.valueOf(r.get("SA")); 
            serviceAccountId = saStr.replace("Service_account: ", "").trim();
            String baStr = String.valueOf(r.get("BA")); 
            billingAccountId = baStr.replace("Billing_account: ", "").trim();
            String caStr = String.valueOf(r.get("CA")); 
            customerAccountId = caStr.replace("Customer Account: ", "").trim();
            
            String lineageBlock = String.valueOf(r.get("CA")) + System.lineSeparator()
                                + String.valueOf(r.get("BA")) + System.lineSeparator()
                                + String.valueOf(r.get("SA"));
            attachText("Account_No fetched from DB(CA/BA/SA)", lineageBlock);
            Allure.step("Extracted  Service Account: " + serviceAccountId);
            Allure.step("Extracted  billing Account: " + billingAccountId);
            Allure.step("Extracted  customer Account: " + customerAccountId);
        }
    }
    private void sleepSeconds(int s) {
        try { Thread.sleep(s * 1000L); } catch (InterruptedException ignored) {}
    }
    
    private Long fetchMainBalance(OracleDB db, Long accountObjId0) {

        String sql =
            "select Current_bal " +
            "from bal_grp_sub_bals_t c, bal_grp_t bb, CONFIG_BEID_BALANCES_T cbd " +
            "where c.obj_id0 = bb.poid_id0 " +
            "and bb.account_obj_id0 = ? " +
            "and REC_ID2 = 978 " +
            "and cbd.rec_id = c.rec_id2";

        return db.getSingleLong(sql, accountObjId0);
    }
    
    @Epic("Prepaid")
    @Feature("Main Balance Topup")
    @Story("Topup with API")
    @Severity(SeverityLevel.CRITICAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 2)
    void TopupBalance() {

        sleepSeconds(10);

        Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
        Assert.assertNotNull(lastAccountObjId0, "Account Obj ID not available from CreateAccount");

        // ---------------- DB Connection ----------------
        OracleDB db = new OracleDB(
                "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
                "pin",
                "BrmDb5#rm#11"
        );

        // ---------------- DB VALIDATION (BEFORE) ----------------
        Long balanceBefore = fetchMainBalance(db, lastAccountObjId0);

        attachDbBalance("DB Balance BEFORE Topup", balanceBefore);
        Allure.step("Fetched DB balance before Topup for account_obj_id0=" + lastAccountObjId0);

        // ---------------- Topup API ----------------
        String topupPayload = "{"
                + "\"description\": \"201\","
                + "\"channel\": {\"name\": \"SMS\",\"id\": \"SMS_01\"},"
                + "\"usageType\": \"978\","
                + "\"relatedParty\": [{\"id\": \"12222-99888-9999\"}],"
                + "\"amount\": {\"amount\": 100,\"units\": \"EUR\"},"
                + "\"logicalResource\": {"
                + "    \"id\": \"" + MSISDN + "\","
                + "    \"name\": \"MSISDN1\""
                + "}"
                + "}";

        attachJson("Topup - Request", topupPayload);
        Allure.step("Initiating topup for MSISDN " + MSISDN + " with amount=100 EUR");

        ValidatableResponse topupVR = ApiRetry.run(
                "Topup",
                () ->
                    given()
                        .relaxedHTTPSValidation()
                        .contentType("application/json")
                        .header("X-Interaction-Id", "dase543553aa")
                        .body(topupPayload)
                        .log().all()
                    .when()
                        .post("https://st.dbss-rm.oci-np.kpn.org/brm/extn/prepayBalanceManagement/v4/topupBalance")
                    .then()
                        .log().ifValidationFails()
                        .statusCode(201)
        );

        attachJson("Topup - Response", topupVR.extract().asString());
        Allure.step("Topup API completed successfully");

        // ---------------- DB VALIDATION (AFTER) ----------------
        sleepSeconds(10); // allow BRM processing

        Long balanceAfter = fetchMainBalance(db, lastAccountObjId0);

        attachDbBalance("DB Balance AFTER Topup", balanceAfter);
        Allure.step("Fetched DB balance after Topup");

        // ---------------- DB ASSERTION ----------------
        Assert.assertNotNull(balanceBefore, "Balance before Topup is NULL");
        Assert.assertNotNull(balanceAfter, "Balance after Topup is NULL");

        Long expectedBalance = balanceBefore + (-100);

        Assert.assertEquals(
                balanceAfter,
                expectedBalance,
                "DB balance mismatch after Topup"
        );

        Allure.step(
            "DB balance validated successfully. " +
            "Before=" + balanceBefore +
            ", After=" + balanceAfter
        );
    }
    
    private List<String> fetchProductNames(OracleDB db, Long accountObjId0) {

        String sql =
            "select pt.name " +
            "from product_t pt, purchased_product_t ppt " +
            "where pt.poid_id0 = ppt.product_obj_id0 " +
            "and ppt.status = 1 "+
            "and ppt.account_obj_id0 = ?";

        List<Map<String, Object>> rows = db.getRows(sql, accountObjId0);

        List<String> products = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            products.add(String.valueOf(row.get("NAME")));
        }

        return products;
    }
    
    private void attachProductList(String title, List<String> products) {

        String content;
        if (products == null || products.isEmpty()) {
            content = "No products found";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < products.size(); i++) {
                sb.append(i + 1).append(". ").append(products.get(i)).append("\n");
            }
            content = sb.toString();
        }

        Allure.addAttachment(title, "text/plain", content);
    }
    
    @Epic("Prepaid")
    @Feature("Adding Supplymentary Offering")
    @Story("Adding Products for SA Account")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = {"CreateAccount"}, priority = 3)
    void AddOffering() {

        sleepSeconds(15);

        Assert.assertNotNull(serviceAccountId, "Service Account ID not captured");
        Assert.assertNotNull(lastAccountObjId0, "Account Obj ID not captured");
        
        Instant now = Instant.now();
        String startDate = DateTimeFormatter.ISO_INSTANT.format(now);
        String endDate = DateTimeFormatter.ISO_INSTANT.format(now.plus(31, ChronoUnit.DAYS));

        // ---------------- DB CONNECTION ----------------
        OracleDB db = new OracleDB(
            "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
            "pin",
            "BrmDb5#rm#11"
        );

        // ================= DB BEFORE =================
        List<String> productsBefore =
            fetchProductNames(db, lastAccountObjId0);

        attachProductList("DB Products BEFORE AddOffering", productsBefore);

        int countBefore = productsBefore.size();
        Allure.step("Products count BEFORE AddOffering = " + countBefore);
        
        String AddOfferingPayload = "{\r\n" +
        		"  \"extension\": {},\r\n" +
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
                "           \"serviceKey\": \"2fa1cd6a-f46d-47ef-a5ac-89ac7cffa17f\",\r\n" +
                "          \"customizedBundles\": [\r\n" +
                "            {\r\n"
                + "              \"customizedChargeOffers\": [\r\n" +
                "                {\r\n" +
                "                  \"name\": \"\",\r\n" +
                "                  \"description\": \"\",\r\n" +
                "                  \"quantity\": 1,\r\n" +
                "                  \"status\": 1,\r\n" +
                "                  \"baseChargeOfferRef\": {\r\n" +
                "                    \"id\": \"kpn_alles_in_1_basis_bundel_001\"\r\n" +
                "                  },\r\n" +
                "                  \"purchaseStart\": {\r\n" +
                "                    \"startDate\": \"" + startDate + "\"\r\n" +
                "                  },\r\n" +
                "                  \"purchaseEnd\": {\r\n" +
                "                    \"endDate\": \"" + endDate + "\"\r\n" +
                "                  },\r\n" +
                "                  \"overriddenCharges\": [\r\n" +
                "                    {\r\n" +
                "                      \"event\": \"" + MSISDN + "\"\r\n" +
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

        // ================= API CALL =================
        attachJson("AddOffering - Request", AddOfferingPayload);
        
        RequestSpecification spec = new RequestSpecBuilder()
        	    .setRelaxedHTTPSValidation()
        	    .setContentType("application/json")
        	    .addHeader("Accept", "*/*")
        	    .addHeader("User-Agent", "PostmanRuntime/7.43.0")
        	    .addHeader("Accept-Encoding", "gzip, deflate, br")
        	    .addHeader("Connection", "keep-alive")
        	    .build();

        ValidatableResponse response = ApiRetry.run(
            "AddOffering",
            () ->
                given()
                    .spec(spec)
                    .body(AddOfferingPayload)
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/changeOffering")
                .then()
                    .statusCode(201)
        );

        attachJson("AddOffering - Response", response.extract().asString());
        Allure.step("AddOffering API executed successfully");

        // ================= DB AFTER =================
        sleepSeconds(10); // allow BRM processing

        List<String> productsAfter =
            fetchProductNames(db, lastAccountObjId0);

        attachProductList("DB Products AFTER AddOffering", productsAfter);

        int countAfter = productsAfter.size();
        Allure.step("Products count AFTER AddOffering = " + countAfter);

        // ================= ASSERTIONS =================
        Assert.assertEquals(
            countAfter,
            countBefore + 1,
            "Product count did not increase by 1 after AddOffering"
        );

        productsAfter.removeAll(productsBefore);
        Assert.assertEquals(productsAfter.size(), 1,
            "Expected exactly one new product");

        Allure.step(
            " DB validated successfully. New product added = " +
            productsAfter.get(0)
        );
    }
    
    
    private String fetchIccidFromDb(OracleDB db, Long accountObjId0) {

        String sql =
            "select st.name as NAME " +
            "from service_t s, service_alias_list_t st " +
            "where s.poid_id0 = st.obj_id0 " +
            "and s.account_obj_id0 = ? " +
            "and st.rec_id = 2";

        return db.getSingleValue(sql, accountObjId0);
    }
    
    private String fetchServiceidFromDb(OracleDB db, Long accountObjId0) {

        String sql =
            "select s.service_id as service_id " +
            "from service_t s, service_alias_list_t st " +
            "where s.poid_id0 = st.obj_id0 " +
            "and s.account_obj_id0 = ? " ;

        return db.getSingleValue(sql, accountObjId0);
    }
    
    private void attachDbValue(String title, String value) {
        Allure.addAttachment(
            title,
            "text/plain",
            value == null ? "NULL" : value
        );
    }

    
    @Epic("Prepaid")
    @Feature("ActivateSIMCard")
    @Story("SIM Swap")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 4)
    void SimSwap_ChangeServiceAttribute() {

        sleepSeconds(8);

        Assert.assertNotNull(MSISDN, "MSISDN not available");
        Assert.assertNotNull(lastAccountObjId0, "accountObjId0 not available");

        OracleDB db = new OracleDB(
            "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
            "pin",
            "BrmDb5#rm#11"
        );

        // ===== DB BEFORE =====
        String iccidBefore = fetchIccidFromDb(db, lastAccountObjId0);
        String Serviceid = fetchServiceidFromDb(db, lastAccountObjId0);
        attachDbValue("DB ICCID BEFORE SIM Swap", iccidBefore);
        Allure.step("ICCID before SIM Swap = " + iccidBefore);

        // ===== NEW ICCID =====
        String iccidDynamic = "10" + Instant.now().getEpochSecond();

        // ===== PAYLOAD (UNCHANGED) =====
        String simSwapPayload =
            "{\n" +
            "  \"operation\": \"UPDATE\",\n" +
            "  \"searchCriteria\": {\n" +
            "    \"entityType\": \"ServiceAccount\",\n" +
            "    \"filters\": {\n" +
            "     \"serviceKey\": \"" + Serviceid + "\" }\n" +
            "  },\n" +
            "  \"partyAccount\": {\n" +
            "    \"@type\": \"ServiceAccount\",\n" +
            "    \"service\": {\n" +
            "      \"aliasList\": [\n" +
            "        { \"name\": \"ICCID1\", \"value\": \"" + iccidDynamic + "\" }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

        attachJson("SIM Swap - Request", simSwapPayload);

        RequestSpecification spec = new RequestSpecBuilder()
            .setRelaxedHTTPSValidation()
            .setContentType("application/json")
            .build();

        ValidatableResponse simSwapVR = ApiRetry.run(
            "SimSwap_ChangeServiceAttribute",
            () -> given()
                    .spec(spec)
                    .body(simSwapPayload)
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/changeServiceAttribute")
                .then()
                    .statusCode(200)
        );

        attachJson("SIM Swap - Response", simSwapVR.extract().asString());

        // ===== DB AFTER =====
        sleepSeconds(10); // allow BRM commit

        String iccidAfter = fetchIccidFromDb(db, lastAccountObjId0);
        attachDbValue("DB ICCID AFTER SIM Swap", iccidAfter);
        Allure.step("ICCID after SIM Swap = " + iccidAfter);

        // ===== ASSERTIONS =====
        Assert.assertNotEquals(
            iccidAfter,
            iccidBefore,
            "ICCID did NOT change after SIM Swap"
        );

        Assert.assertEquals(
            iccidAfter,
            iccidDynamic,
            "ICCID in DB does not match new ICCID from request"
        );

        Allure.step("SIM Swap verified in DB. ICCID updated successfully.");
    }

    
    @Epic("Prepaid")
    @Feature("Change Offering")
    @Story("Add & Remove Chargeoffers")
    @Severity(SeverityLevel.NORMAL)
    @Test(priority = 12, dependsOnMethods = "CreateAccount") 
    void ChangeOffering_AddDiscount_RemoveCharge() {
    //	sleepSeconds(8);
    	RestAssured.config = RestAssured.config()
                .redirect(RedirectConfig.redirectConfig().followRedirects(true));
        
        Assert.assertNotNull(serviceAccountId, "Service Account ID not captured from CreateAccount test");
        String accountId = serviceAccountId;

        
        Assert.assertNotNull(lastAccountObjId0, "Numeric account_obj_id0 not captured from CreateAccount test");

        
        String jdbcUrl = "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com";
        String dbUser = "pin";
        String dbPass = "BrmDb5#rm#11";
        OracleDB db = new OracleDB(jdbcUrl, dbUser, dbPass);

        String discountOfferCode = "kpn_alles_in_1_basis_bundel_001";

        String uniqueIdSql =
            "select ppt.unique_id " +
            "from product_t pt , purchased_product_t ppt " +
            "where pt.poid_id0 = ppt.product_obj_id0 " +
            "  and ppt.account_obj_id0 = ? " +      
            "  and pt.code = ?";

        
        String uniqueId = db.getSingleValue(uniqueIdSql, lastAccountObjId0, discountOfferCode);
        Assert.assertNotNull(uniqueId, "UNIQUE_ID not found for account_obj_id0=" + lastAccountObjId0 + " and code=" + discountOfferCode);

        Allure.step("Fetched UNIQUE_ID for the existing product from DB: " + uniqueId);
        Allure.addAttachment("DB Query (for fetching UNIQUE_ID)", uniqueIdSql +
            "\n\nParams:\naccount_obj_id0=" + lastAccountObjId0 + "\n");
        

        List<String> productsBefore = fetchProductNames(db, lastAccountObjId0);
        attachProductList("DB Products BEFORE ChangeOffering", productsBefore);
        Allure.step("Fetched product list BEFORE ChangeOffering");


        
        String currentTs = String.valueOf(java.time.Instant.now().getEpochSecond());

        String payload = "{\r\n" +
        		"  \"extension\": {},\r\n" +
                "  \"operation\": \"ADD\",\r\n" +
                "  \"searchCriteria\": {\r\n" +
                "    \"entityType\": \"ServiceAccount\",\r\n" +
                "    \"filters\": {\r\n" +
                "      \"accountId\": \"" + accountId + "\"\r\n" +
                "    }\r\n" +
                "  },\r\n" +
                "  \"product\": [\r\n" +
                "    {\r\n" +
                "      \"actionType\": \"ADD\",\r\n" +
                "      \"services\": [\r\n" +
                "        {\r\n" +
                "          \"serviceType\": \"/service/telco/gsm\",\r\n" +
                "           \"serviceKey\": \"2fa1cd6a-f46d-47ef-a5ac-89ac7cffa17f\",\r\n" +
                "          \"customizedBundles\": [\r\n" +
                "            {\r\n" +
                "              \"customizedChargeOffers\": [\r\n" +
                "                {\r\n" +
                "                  \"name\": \"\",\r\n" +
                "                  \"description\": \"\",\r\n" +
                "                  \"quantity\": 1,\r\n" +
                "                  \"status\": 1,\r\n" +
                "                  \"baseChargeOfferRef\": {\r\n" +
                "                    \"id\": \"kpn_onbeperkt_online_promotie_bundel_001\" \r\n" +
                "                  },\r\n" +
                "                  \"purchaseStart\": { \"startDate\": \"\" },\r\n" +
                "                  \"purchaseEnd\":   { \"endDate\":   \"\" },\r\n" +
                "                  \"overriddenCharges\": [\r\n" +
                "                    { \"event\": \"" + currentTs + "\" }\r\n" +
                "                  ]\r\n" +
                "                }\r\n" +
                "              ]\r\n" +
                "            }\r\n" +
                "          ],\r\n" +
                "          \"subscriptionIndex\": 0\r\n" +
                "        }\r\n" +
                "      ]\r\n" +
                "    },\r\n" +
                "    {\r\n" +
                "      \"actionType\": \"REMOVE\",\r\n" +
                "      \"services\": [\r\n" +
                "        {\r\n" +
                "          \"serviceType\": \"/service/telco/gsm\",\r\n" +
                "          \"serviceKey\": \"" + discountOfferCode + "\",\r\n" +
                "          \"customizedBundles\": [\r\n" +
                "            {\r\n" +
                "              \"customizedChargeOffers\": [\r\n" +
                "                {\r\n" +
                "                  \"quantity\": 1,\r\n" +
                "                  \"overriddenCharges\": [\r\n" +
                "                    { \"event\": \"" + uniqueId  + "\" }\r\n" +
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

        Allure.addAttachment("ChangeOffering (CHANGEOFFERING) - Request", "application/json", payload, java.nio.charset.StandardCharsets.UTF_8.name());
        Allure.step("Calling CHANGEOFFERING API with | accountId=" + accountId + " | Product_code to be removed=" + discountOfferCode +
            " | (Unique_ID for existing Product=" + uniqueId + ", Unique_ID for new Product=" + currentTs + ")");
        
        RequestSpecification spec = new RequestSpecBuilder()
        	    .setRelaxedHTTPSValidation()
        	    .setContentType("application/json")
        	    .addHeader("Accept", "*/*")
        	    .addHeader("User-Agent", "PostmanRuntime/7.43.0")
        	    .addHeader("Accept-Encoding", "gzip, deflate, br")
        	    .addHeader("Connection", "keep-alive")
        	    .build();
        
        io.restassured.response.ValidatableResponse vr =
            given()
            	.spec(spec)
                .body(payload)
                .log().all()
            .when()
                .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/changeOffering")
            .then()
                .log().ifValidationFails()
                .statusCode(201)
                .log().all();

        String respBody = vr.extract().asString();
        Allure.addAttachment("ChangeOffering (CHANGEOFFERING) - Response", "application/json", respBody, java.nio.charset.StandardCharsets.UTF_8.name());
        Allure.step("CHANGEOFFERING completed | accountId=" + accountId + "");
        

		List<String> productsAfter = fetchProductNames(db, lastAccountObjId0);
		attachProductList("DB Products AFTER ChangeOffering", productsAfter);
		Allure.step("Fetched product list AFTER ChangeOffering");

    }
    
    

private BigDecimal fetchBalance(OracleDB db, Long accountObjId0) {

    String sql =
        "select c.current_bal as CURRENT_BAL " +
        "from bal_grp_sub_bals_t c, bal_grp_t bb, CONFIG_BEID_BALANCES_T cbd " +
        "where c.obj_id0 = bb.poid_id0 " +
        "and bb.account_obj_id0 = ? " +
        "and c.rec_id2 = 978 " +
        "and cbd.rec_id = c.rec_id2";

    String value = db.getSingleValue(sql, accountObjId0);
    return (value == null) ? null : new BigDecimal(value);
}

private void attachOraDbBalance(String title, BigDecimal balance) {
    Allure.addAttachment(
        title,
        "text/plain",
        "Current Balance = " + balance.toPlainString()
    );
}

    @Epic("Prepaid")
    @Feature("Balance Transfer")
    @Story("Balance Transfer between Service Accounts")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 8)
    void BalanceTransfer() {

        sleepSeconds(8);

        // ---------- PRE-CHECKS ----------
        Assert.assertNotNull(serviceAccountId, "Service Account ID not captured");
        Assert.assertNotNull(lastAccountObjId0, "Account Obj ID not available");

        // ---------- DB CONNECTION ----------
        OracleDB db = new OracleDB(
            "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
            "pin",
            "BrmDb5#rm#11"
        );

        // ========== DB BEFORE ==========
        BigDecimal balanceBefore = fetchBalance(db, lastAccountObjId0);
        Assert.assertNotNull(balanceBefore, "Balance BEFORE is NULL");

        attachOraDbBalance("DB Balance BEFORE BalanceTransfer", balanceBefore);
        Allure.step("DB balance before transfer = " + balanceBefore);

        // ---------- API PAYLOAD ----------
        String payload =
            "{\n" +
            "  \"operation\": \"UPDATE\",\n" +
            "  \"searchCriteria\": {\n" +
            "    \"entityType\": \"ServiceAccount\",\n" +
            "    \"filters\": {\n" +
            "      \"accountId\": \"SA86789712\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"relatedEntities\": [\n" +
            "    {\n" +
            "      \"entityType\": \"ServiceAccount\",\n" +
            "      \"filters\": {\n" +
            "        \"accountId\": \"" + serviceAccountId + "\"\n" +
            "      },\n" +
            "      \"role\": \"target\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"transferDetail\": {\n" +
            "    \"amount\": {\n" +
            "      \"amount\": 1,\n" +
            "      \"units\": \"EUR\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

        attachJson("BalanceTransfer - Request", payload);

        // ---------- REQUEST SPEC ----------
        RequestSpecification spec = new RequestSpecBuilder()
            .setRelaxedHTTPSValidation()
            .setContentType("application/json")
            .addHeader("Accept", "*/*")
            .addHeader("User-Agent", "PostmanRuntime/7.43.0")
            .addHeader("X-Interaction-Id", "dase543553aa")
            .addHeader("X-Order-Type", "BalanceTransfer")
            .addHeader("Connection", "keep-alive")
            .build();

        RestAssured.config = RestAssured.config()
            .redirect(RedirectConfig.redirectConfig().followRedirects(true));

        // ---------- API CALL ----------
        ValidatableResponse vr = ApiRetry.run(
            "BalanceTransfer",
            () ->
                given()
                    .spec(spec)
                    .body(payload)
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/transferBalance")
                .then()
                    .statusCode(200)
        );

        attachJson("BalanceTransfer - Response", vr.extract().asString());
        Allure.step("BalanceTransfer API executed successfully");

        // ========== DB AFTER ==========
        sleepSeconds(10); // allow BRM processing

        BigDecimal balanceAfter = fetchBalance(db, lastAccountObjId0);
        Assert.assertNotNull(balanceAfter, "Balance AFTER is NULL");

        attachOraDbBalance("DB Balance AFTER BalanceTransfer", balanceAfter);
        Allure.step("DB balance after transfer = " + balanceAfter);

        // ========== ASSERTION ==========

        BigDecimal expectedBalance =
        balanceBefore.subtract(BigDecimal.ONE);



Assert.assertEquals(
    balanceAfter.compareTo(expectedBalance),
    0,
    "Balance mismatch after BalanceTransfer"
);


        Allure.step(
            "DB validation successful. Balance changed correctly: " +
            balanceBefore + " → " + balanceAfter
        );
    }

    
    private String fetchNotificationLanguage(OracleDB db, Long accountObjId0) {

        String sql =
            "select ps.value as VALUE " +
            "from profile_t p, profile_subscriber_prefs_t ps " +
            "where p.poid_id0 = ps.obj_id0 " +
            "and ps.name = 'NOTIFICATION_LANGUAGE' " +
            "and p.account_obj_id0 = ?";

        return db.getSingleValue(sql, accountObjId0);
    }
    @Epic("Prepaid")
    @Feature("Profile Management")
    @Story("Update Service Account Profile")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 6)
    void UpdateProfile() {

        sleepSeconds(8);

        Assert.assertNotNull(serviceAccountId, "Service Account ID not captured");
        Assert.assertNotNull(lastAccountObjId0, "Account Obj ID not available");

        // ---------- DB CONNECTION ----------
        OracleDB db = new OracleDB(
            "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
            "pin",
            "BrmDb5#rm#11"
        );

        // ========== DB BEFORE ==========
        String languageBefore = fetchNotificationLanguage(db, lastAccountObjId0);
        attachDbValue("DB NOTIFICATION_LANGUAGE BEFORE UpdateProfile", languageBefore);
        Allure.step("NOTIFICATION_LANGUAGE before update = " + languageBefore);

        // ---------- PAYLOAD (UNCHANGED) ----------
        String payload = "{\r\n" +
                "  \"operation\": \"UPDATE\",\r\n" +
                "  \"searchCriteria\": {\r\n" +
                "    \"entityType\": \"ServiceAccount\",\r\n" +
                "    \"filters\": {\r\n" +
                "      \"accountId\": \"" + serviceAccountId + "\"\r\n" +
                "    }\r\n" +
                "  },\r\n" +
                "  \"partyAccount\": {\r\n" +
                "    \"@type\": \"ServiceAccount\",\r\n" +
                "    \"accountType\": \"3\",\r\n" +
                "    \"isPermanent\": \"false\",\r\n" +
                "    \"accountTag\": \"PREMIUM\",\r\n" +
                "    \"description\": \"Premium service\",\r\n" +
                "    \"profile\": {\r\n" +
                "      \"subscriberPreference\": [\r\n" +
                "        { \"name\": \"NOTIFICATION_CHANNEL\", \"value\": \"SMS\" },\r\n" +
                "        { \"name\": \"NOTIFICATION_LANGUAGE\", \"value\": \"Dutch\" },\r\n" +
                "        { \"name\": \"NOTIFICATION_MSISDN\", \"value\": \"45345345\" },\r\n" +
                "        { \"name\": \"NOTIFICATION_EMAIL\",  \"value\": \"test@gmail.com\" },\r\n" +
                "        { \"name\": \"KPN_PAY\",  \"value\": \"Y\" },\r\n" +
                "        { \"name\": \"BRAND_ID\", \"value\": \"201\" },\r\n" +
                "        { \"name\": \"KPNPAY_CAP\", \"value\": \"300\" },\r\n" +
                "        { \"name\": \"ROAMING_CAP\", \"value\": \"50\" },\r\n" +
                "        { \"name\": \"SILENTHOUR_START\", \"value\": \"9AM\" },\r\n" +
                "        { \"name\": \"SILENTHOUR_END\",   \"value\": \"9PM\" }\r\n" +
                "      ]\r\n" +
                "    }\r\n" +
                "  }\r\n" +
                "}";

        attachJson("UpdateProfile - Request", payload);

        RequestSpecification spec = new RequestSpecBuilder()
            .setRelaxedHTTPSValidation()
            .setContentType("application/json")
            .build();

        ValidatableResponse vr = ApiRetry.run(
            "UpdateProfile",
            () -> given()
                    .spec(spec)
                    .body(payload)
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/updateProfile")
                .then()
                    .statusCode(200)
        );

        attachJson("UpdateProfile - Response", vr.extract().asString());
        Allure.step("Profile updated successfully for ServiceAccount=" + serviceAccountId);

        // ========== DB AFTER ==========
        sleepSeconds(10); // allow DB commit

        String languageAfter = fetchNotificationLanguage(db, lastAccountObjId0);
        attachDbValue("DB NOTIFICATION_LANGUAGE AFTER UpdateProfile", languageAfter);
        Allure.step("NOTIFICATION_LANGUAGE after update = " + languageAfter);

        // ========== ASSERTION ==========
        Assert.assertEquals(
            languageAfter,
            "Dutch",
            "NOTIFICATION_LANGUAGE not updated correctly in DB"
        );

        Allure.step("Profile DB validation successful. Language updated to Dutch.");
    }
    
    private String fetchKpnPayCap(OracleDB db, Long accountObjId0) {

        String sql =
            "select ps.value as VALUE " +
            "from profile_t p, profile_subscriber_prefs_t ps " +
            "where p.poid_id0 = ps.obj_id0 " +
            "and ps.name = 'KPNPAY_CAP' " +
            "and p.account_obj_id0 = ?";

        return db.getSingleValue(sql, accountObjId0);
    }
    
    @Epic("Prepaid")
    @Feature("Subscriber Profile")
    @Story("ChangeCreditLimit")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 7)
    void ChangeCreditLimit() {

        Assert.assertNotNull(serviceAccountId, "Service Account ID not captured");
        Assert.assertNotNull(lastAccountObjId0, "Account Obj ID not available");

        // ---------- DB CONNECTION ----------
        OracleDB db = new OracleDB(
            "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
            "pin",
            "BrmDb5#rm#11"
        );

        // ================= DB BEFORE =================
        String capBefore = fetchKpnPayCap(db, lastAccountObjId0);
        attachDbValue("DB KPNPAY_CAP BEFORE ChangeCreditLimit", capBefore);
        Allure.step("KPNPAY_CAP before update = " + capBefore);

        // ---------- PAYLOAD (UNCHANGED) ----------
        String payload = "{\r\n" +
                "  \"operation\": \"UPDATE\",\r\n" +
                "  \"searchCriteria\": {\r\n" +
                "    \"entityType\": \"ServiceAccount\",\r\n" +
                "    \"filters\": {\r\n" +
                "      \"accountId\": \"" + serviceAccountId + "\"\r\n" +
                "    }\r\n" +
                "  },\r\n" +
                "  \"partyAccount\": {\r\n" +
                "    \"@type\": \"ServiceAccount\",\r\n" +
                "    \"accountType\": \"3\",\r\n" +
                "    \"isPermanent\": \"false\",\r\n" +
                "    \"accountTag\": \"PREMIUM\",\r\n" +
                "    \"description\": \"Premium service\",\r\n" +
                "    \"profile\": {\r\n" +
                "      \"subscriberPreference\": [\r\n" +
                "        { \"name\": \"KPNPAY_CAP\", \"value\": \"500\" }\r\n" +
                "      ]\r\n" +
                "    }\r\n" +
                "  }\r\n" +
                "}";

        attachJson("ChangeCreditLimit - Request", payload);

        RequestSpecification spec = new RequestSpecBuilder()
            .setRelaxedHTTPSValidation()
            .setContentType("application/json")
            .build();

        ValidatableResponse vr = ApiRetry.run(
            "ChangeCreditLimit",
            () -> given()
                    .spec(spec)
                    .body(payload)
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/updateProfile")
                .then()
                    .statusCode(200)
        );

        attachJson("ChangeCreditLimit - Response", vr.extract().asString());
        Allure.step("ChangeCreditLimit API executed successfully");

        // ================= DB AFTER =================
        sleepSeconds(10); // allow DB commit

        String capAfter = fetchKpnPayCap(db, lastAccountObjId0);
        attachDbValue("DB KPNPAY_CAP AFTER ChangeCreditLimit", capAfter);
        Allure.step("KPNPAY_CAP after update = " + capAfter);

        // ================= ASSERTION =================
        Assert.assertEquals(
            capAfter,
            "500",
            "KPNPAY_CAP not updated correctly in DB"
        );

        Allure.step("KPNPAY_CAP DB validation successful. Value updated to 500.");
    }
    
    

private BigDecimal fetchKpnPayBalance(OracleDB db, Long accountObjId0) {

    String sql =
        "select c.current_bal as CURRENT_BAL " +
        "from bal_grp_sub_bals_t c, bal_grp_t bb, CONFIG_BEID_BALANCES_T cbd " +
        "where c.obj_id0 = bb.poid_id0 " +
        "and bb.account_obj_id0 = ? " +
        "and c.rec_id2 = 1000104 " +
        "and cbd.rec_id = c.rec_id2";


String value = db.getSingleValue(sql, accountObjId0);

   
    if (value == null) {
        return BigDecimal.ZERO;
    }

    return new BigDecimal(value);

}
@Epic("Prepaid")
@Feature("KPNPAY_TRANSACTION")
@Story("Create KPN PAY")
@Severity(SeverityLevel.NORMAL)
@Test(dependsOnMethods = "CreateAccount", priority = 9)
void KPN_PAY_CREATE() {

    sleepSeconds(8);

    // ---------- PRE-CONDITIONS ----------
    Assert.assertNotNull(serviceAccountId, "Service Account ID not captured");
    Assert.assertNotNull(lastAccountObjId0, "Account Obj ID not available");

    // ---------- DB CONNECTION ----------
    OracleDB db = new OracleDB(
        "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
        "pin",
        "BrmDb5#rm#11"
    );

    // ========== DB BEFORE ==========
    BigDecimal balanceBefore = fetchKpnPayBalance(db, lastAccountObjId0);
    Assert.assertNotNull(balanceBefore, "KPN PAY balance BEFORE is NULL");

    attachOraDbBalance("DB KPN PAY Balance BEFORE", balanceBefore);
    Allure.step("KPN PAY balance before usage = " + balanceBefore);

    // ---------- DATE ----------
    Instant now = Instant.now();
    String usageDate = DateTimeFormatter.ISO_INSTANT.format(now);

    // ---------- PAYLOAD (UNCHANGED STRUCTURE) ----------
    String payload =
        "{\n" +
        "  \"description\": \"Transaction_PP011225_1\",\n" +
        "  \"usageType\": \"KPN_PAY\",\n" +
        "  \"usageDate\": \"" + usageDate + "\",\n" +
        "  \"relatedParty\": [\n" +
        "    {\n" +
        "      \"role\": \"ServiceAccount\",\n" +
        "      \"id\": \"" + serviceAccountId + "\",\n" +
        "      \"@referredType\": \"ServiceAccount\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"usageCharacteristic\": [\n" +
        "    {\n" +
        "      \"name\": \"requestId\",\n" +
        "      \"value\": \"77777\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"ratedProductUsage\": [\n" +
        "    {\n" +
        "      \"productRef\": {\n" +
        "        \"id\": \"Google\",\n" + // Updated to Match curl definition
        "        \"name\": \"KPN PAY Google store Downloads\"\n" + // Updated to Match curl definition
        "      },\n" +
        "      \"taxIncludedRatingAmount\": {\n" +
        "        \"value\": 2,\n" +
        "        \"unit\": \"EUR\"\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}";

    attachJson("KPNPAY_CREATE - Request", payload);
    Allure.step("Creating KPN PAY usage for ServiceAccount=" + serviceAccountId);

    // ---------- API CALL ----------
    ValidatableResponse vr = ApiRetry.run(
        "KPN_PAY_CREATE",
        () ->
            given()
                .relaxedHTTPSValidation()
                .contentType("application/json")
                .header("X-Interaction-Id", "a4536gdghd") // Updated header configuration from curl
                .header("X-Source-System", "BOKU")      // Added header configuration from curl
                .body(payload)
            .when()
                .post("https://st.dbss-rm.oci-np.kpn.org/brm/extn/usageManagement/v4/kpnPay") // Updated endpoint URI to match HTTP curl definition
            .then()
                .log().ifValidationFails()
                .statusCode(201)
    );

    String respBody = vr.extract().response().asPrettyString();
    attachJson("KPNPAY_CREATE - Response", respBody);
    
    System.out.println("----- KPN PAY CREATE RESPONSE START -----");
    System.out.println(respBody);
    System.out.println("----- KPN PAY CREATE RESPONSE END -----");
    
    Allure.step("KPN PAY usage created successfully");

    // ========== DB AFTER ==========
    sleepSeconds(10); // allow BRM processing

    BigDecimal balanceAfter = fetchKpnPayBalance(db, lastAccountObjId0);
    Assert.assertNotNull(balanceAfter, "KPN PAY balance AFTER is NULL");

    attachOraDbBalance("DB KPN PAY Balance AFTER", balanceAfter);
    Allure.step("KPN PAY balance after usage = " + balanceAfter);

    // ========== ASSERTION ==========
    BigDecimal expectedBalance = balanceBefore.subtract(new BigDecimal("-2"));

    Assert.assertEquals(
        balanceAfter.compareTo(expectedBalance),
        0,
        "KPN PAY balance mismatch after usage"
    );

    Allure.step(
        "DB validation successful. KPN PAY balance updated correctly: " +
        balanceBefore + " → " + balanceAfter
    );
}
    
@Epic("Prepaid")
@Feature("KPNPAY_TRANSACTION_REFUND")
@Story("Refund KPN PAY")
@Severity(SeverityLevel.NORMAL)
@Test(dependsOnMethods = "CreateAccount", priority = 10)
void KPN_PAY_REFUND() {

    sleepSeconds(8);

    // ---------- PRE-CONDITIONS ----------
    Assert.assertNotNull(serviceAccountId, "Service Account ID not captured");
    Assert.assertNotNull(lastAccountObjId0, "Account Obj ID not available");

    // ---------- DB CONNECTION ----------
    OracleDB db = new OracleDB(
        "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
        "pin",
        "BrmDb5#rm#11"
    );

    // ================= DB BEFORE =================
    BigDecimal balanceBefore = fetchKpnPayBalance(db, lastAccountObjId0);
    Assert.assertNotNull(balanceBefore, "KPN PAY balance BEFORE is NULL");

    attachOraDbBalance("DB KPN PAY Balance BEFORE REFUND", balanceBefore);
    Allure.step("KPN PAY balance before refund = " + balanceBefore);

    // ---------- DATE ----------
    Instant now = Instant.now();
    String usageDate = DateTimeFormatter.ISO_INSTANT.format(now);

    // ---------- PAYLOAD (UNCHANGED STRUCTURE) ----------
    String payload =
        "{\n" +
        "  \"description\": \"Refund for Transaction_PP011225_1\",\n" +
        "  \"usageType\": \"KPN_REFUND\",\n" +
        "  \"usageDate\": \"" + usageDate + "\",\n" +
        "  \"relatedParty\": [\n" +
        "    {\n" +
        "      \"role\": \"ServiceAccount\",\n" +
        "      \"id\": \"" + serviceAccountId + "\",\n" +
        "      \"@referredType\": \"ServiceAccount\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"usageCharacteristic\": [\n" +
        "    { \"name\": \"requestId\", \"value\": \"88888\" },\n" +
        "    { \"name\": \"relatedRequestId\", \"value\": \"77777\" },\n" +
        "    { \"name\": \"dbssTransactionId\", \"value\": \"361273132651709351\" }\n" +
        "  ],\n" +
        "  \"ratedProductUsage\": [\n" +
        "    {\n" +
        "      \"productRef\": {\n" +
        "        \"id\": \"Apple\",\n" + // Updated to Match curl definition
        "        \"name\": \"KPN PAY Apple store Downloads\"\n" +
        "      },\n" +
        "      \"taxIncludedRatingAmount\": {\n" +
        "        \"value\": 1,\n" +
        "        \"unit\": \"EUR\"\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}";

    attachJson("KPNPAY_REFUND - Request", payload);

    // ---------- API CALL ----------
    ValidatableResponse vr = ApiRetry.run(
        "KPNPAY_REFUND",
        () ->
            given()
                .relaxedHTTPSValidation()
                .contentType("application/json")
                .header("X-Interaction-id", "adae54645v") // Updated header configuration from curl
                .header("X-SOURCE-SYSTEM", "BOKU")      // Added header configuration from curl
                .body(payload)
            .when()
                .post("https://st.dbss-rm.oci-np.kpn.org/brm/extn/usageManagement/v4/kpnPay") // Updated endpoint URI to match HTTP curl definition
            .then()
                .log().ifValidationFails()
                .statusCode(200)
    );

    String respBody = vr.extract().response().asPrettyString();
    attachJson("KPNPAY_REFUND - Response", respBody);
    
    System.out.println("----- KPN PAY REFUND RESPONSE START -----");
    System.out.println(respBody);
    System.out.println("----- KPN PAY REFUND RESPONSE END -----");
    
    Allure.step("KPN PAY refund API executed successfully");

    // ================= DB AFTER =================
    sleepSeconds(10); // allow BRM processing

    BigDecimal balanceAfter = fetchKpnPayBalance(db, lastAccountObjId0);
    Assert.assertNotNull(balanceAfter, "KPN PAY balance AFTER is NULL");

    attachOraDbBalance("DB KPN PAY Balance AFTER REFUND", balanceAfter);
    Allure.step("KPN PAY balance after refund = " + balanceAfter);

    // ================= ASSERTION =================
    BigDecimal expectedAfterRefund = balanceBefore.subtract(BigDecimal.ONE);

    Assert.assertEquals(
        balanceAfter.compareTo(expectedAfterRefund),
        0,
        "KPN PAY balance mismatch after REFUND"
    );

    Allure.step(
        "DB validation successful. KPN PAY balance refunded correctly: " +
        balanceBefore + " → " + balanceAfter
    );
}
    
   
    @Epic("Prepaid")
    @Feature("TopupBalance_WithVoucher")
    @Story("Voucher redemption")
    @Severity(SeverityLevel.NORMAL)
    
   
    @Test(dependsOnMethods = "CreateAccount", priority = 11)
    public void TopupBalance_WithVoucher_SMS() {
        // If you want to use the dynamically created MSISDN from your suite:
        Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");

        // --------- Option A: Use dynamic MSISDN & your own voucher/amount/units ----------
        String interactionId = "123-un78dsdsdsd-sdsds";
        String cookieValue   = "nginxingresscookie=0d70706d807ed83bd5d726961a21d4b0|8038bf2692513c815d85f28f02a6ef15";
        String voucher       = "2394905452474604";
        
        String unitsCode     = "2000004"; // per curl sample


			int amountValue = 60;
			String units = "EUR";
			String description = "201";
			String channelName = "SMS";
			String channelId = "SMS_01";
			String usageType = "2000004";
			String relatedPartyId = "12222-99888-9999";
			
			String payload = "{\r\n" +
			    "  \"voucher\": \"" + voucher + "\",\r\n" +
			    "  \"description\": \"" + description + "\",\r\n" +
			    "  \"channel\": {\r\n" +
			    "    \"name\": \"" + channelName + "\",\r\n" +
			    "    \"id\": \"" + channelId + "\"\r\n" +
			    "  },\r\n" +
			    "  \"usageType\": \"" + usageType + "\",\r\n" +
			    "  \"relatedParty\": [\r\n" +
			    "    { \"id\": \"" + relatedPartyId + "\" }\r\n" +
			    "  ],\r\n" +
			    "  \"amount\": {\r\n" +
			    "    \"amount\": " + amountValue + ",\r\n" +
			    "    \"units\": \"" + units + "\"\r\n" +
			    "  },\r\n" +
			    "  \"logicalResource\": {\r\n" +
			    "    \"id\": \"" + MSISDN + "\",\r\n" +
			    "    \"name\": \"MSISDN1\"\r\n" +
			    "  }\r\n" +
			    "}";



        // Attach to Allure (optional)
        attachJson("Topup (Voucher/SMS) - Request", payload);
        Allure.step("Topup (Voucher) via SMS for MSISDN=" + MSISDN + ", amount=" + amountValue + " " + unitsCode);

        // Configure redirect following (equivalent to curl -L)
        RestAssured.config = RestAssured.config()
            .redirect(RedirectConfig.redirectConfig().followRedirects(true));

        ValidatableResponse vr =
            given()
                .relaxedHTTPSValidation() 
                .contentType("application/json")
                .accept("application/json")
                .header("kpn-interaction-id", interactionId)
                .header("Cookie", cookieValue)
                .body(payload)
                .log().all()
            .when()
                .post("https://st.dbss-rm.oci-np.kpn.org/brm/extn/prepayBalanceManagement/v4/topupBalance")
            .then()
                .log().ifValidationFails()
               .statusCode(201); 

        String resp = vr.extract().asString();
        attachJson("Topup (Voucher/SMS) - Response", resp);
        Allure.step("Topup (Voucher) completed successfully for " + MSISDN);
    }
    
    @Epic("Prepaid")
    @Feature("Apply Adjustment")
    @Story("Adjustments")
    @Severity(SeverityLevel.NORMAL)
    
    @Test(dependsOnMethods = {"CreateAccount"}, priority = 5)
    public void AdjustAccount() {
        // If you prefer to use MSISDN from CreateAccount, uncomment and use it:
        // Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
        // String serviceId = MSISDN;
    	Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
        // ---- Values from your curl (you can parameterize if needed) ----
        String cookie = "nginxingresscookie=93d5530647235dde23fdfa121cfca531|6f56107e206dbfb962b8bcaf43728f23";
         // or use MSISDN captured earlier
        
        boolean amountIsCredit = true;               // false = debit; true = credit
        double amount = 1.0;
        boolean includeTax = false;
        Instant now = Instant.now();
        String effective = DateTimeFormatter.ISO_INSTANT.format(now);
        
        String payload = "{\n" +
                "  \"extension\": {\n" +
                "    \"billWhen\": 0,\n" +
                "    \"balanceType\": \"KPN_Prepaid_Initial_Balance\",\n" +
                "    \"reasonCode\": \"one-off service\",\n" +
                "    \"invoiceText\": \"Adjusted with Bill\"\n" +
                "  },\n" +
                "  \"effective\": \"" + effective + "\",\n" +
                "  \"amountIsCredit\": " + amountIsCredit + ",\n" +
                "  \"notes\": {\n" +
                "    \"serviceId\": \"" + MSISDN + "\",\n" +
                "    \"comments\": [ { \"comment\": \"\" } ]\n" +
                "  },\n" +
                "  \"amount\": " + amount + ",\n" +
                "  \"includeTax\": " + includeTax + "\n" +
                "}";

        // ---- Allure attachments (optional) ----
        attachJson("AdjustAccount - Request", payload);
        Allure.step("AdjustAccount (Initial) for serviceId=" + MSISDN +
                    " | amount=" + amount +
                    (amountIsCredit ? " (credit)" : " (debit)") +
                    " | effective=" + effective);

        // ---- Follow redirects if any (optional) ----
        RestAssured.config = RestAssured.config()
                .redirect(RedirectConfig.redirectConfig().followRedirects(true));

        ValidatableResponse vr =
            given()
                .relaxedHTTPSValidation()
                .contentType("application/json")
                .accept("application/json")
                .header("X-Interaction-Id", "dase543553aa")	
                .header("Cookie", cookie)
                .body(payload)
                .log().all()
            .when()
                .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/adjustAccount")
            .then()
                .log().ifValidationFails()
                .statusCode(201); // adjust if your API returns a different success code

        String resp = vr.extract().asString();
        attachJson("AdjustAccount - Response", resp);
        Allure.step("AdjustAccount completed for serviceId=" + MSISDN);
    }
    
    
    @Epic("Prepaid")
    @Feature("Bundle Balance")
    @Story("Get Bundle Balance")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 13)
    void GetBundleBalance() {
 
        Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
 
        String payload =
            "{\r\n" +
            "  \"operation\": \"SEARCH\",\r\n" +
            "  \"searchCriteria\": {\r\n" +
            "    \"entityType\": \"ServiceAccount\",\r\n" +
            "    \"filters\": {\r\n" +
            "      \"aliasList\": [\r\n" +
            "        {\r\n" +
            "          \"name\": \"MSISDN1\",\r\n" +
            "          \"value\": \"" + MSISDN + "\"\r\n" +
            "        }\r\n" +
            "      ]\r\n" +
            "    }\r\n" +
            "  }\r\n" +
            "}";
 
        attachJson("GetBundleBalance - Request", payload);
        Allure.step("Fetching bundle balance for MSISDN=" + MSISDN);
        
        RequestSpecification spec = new RequestSpecBuilder()
        	    .setRelaxedHTTPSValidation()
        	    .setContentType("application/json")
        	    .addHeader("Accept", "*/*")
        	    .addHeader("User-Agent", "PostmanRuntime/7.43.0")
        	    .addHeader("Accept-Encoding", "gzip, deflate, br")
        	    .addHeader("Connection", "keep-alive")
        	    .build();
        
        RestAssured.config = RestAssured.config()
                .redirect(RedirectConfig.redirectConfig().followRedirects(true));
        
        
        
        ValidatableResponse vr = ApiRetry.run(
                "GetBundleBalance",
                () ->
                    given()
                        .spec(spec)
                        .body(payload)
                    .when()
                        .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getBundleBalance")
                    .then()
                    	.log().ifValidationFails()
                        .statusCode(200)
                        .log().all()
        );
 
        String respBody = vr.extract().asString();
        attachJson("GetBundleBalance - Response", respBody);
        Allure.step("Bundle Balance fetched successfully for MSISDN=" + MSISDN);
    }
 
    @Epic("Prepaid")
    @Feature("Offering Information")
    @Story("Get Offering Details")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 14)
    void GetOffering() {
 
        Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
 
        String payload =
            "{\r\n" +
            "  \"operation\": \"SEARCH\",\r\n" +
            "  \"searchCriteria\": {\r\n" +
            "    \"entityType\": \"ServiceAccount\",\r\n" +
            "    \"filters\": {\r\n" +
            "      \"aliasList\": [\r\n" +
            "        {\r\n" +
            "          \"name\": \"MSISDN1\",\r\n" +
            "          \"value\": \"" + MSISDN + "\"\r\n" +
            "        }\r\n" +
            "      ]\r\n" +
            "    }\r\n" +
            "  }\r\n" +
            "}";
 
        attachJson("GetOffering - Request", payload);
        Allure.step("Fetching Offering details for MSISDN=" + MSISDN);
        
        RestAssured.config = RestAssured.config()
                .redirect(RedirectConfig.redirectConfig().followRedirects(true));
 
        RequestSpecification spec = new RequestSpecBuilder()
        	    .setRelaxedHTTPSValidation()
        	    .setContentType("application/json")
        	    .addHeader("Accept", "*/*")
        	    .addHeader("User-Agent", "PostmanRuntime/7.43.0")
        	    .addHeader("Accept-Encoding", "gzip, deflate, br")
        	    .addHeader("Connection", "keep-alive")
        	    .build();
        
        ValidatableResponse vr = ApiRetry.run(
                "GetOffering",
                () ->
                    given()
                        .spec(spec)
                        .body(payload)
                    .when()
                        .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getSubscriberInfo")
                    .then()
                    	.log().ifValidationFails()
                        .statusCode(200)
                        .log().all()
        );
 
        String respBody = vr.extract().asString();
        attachJson("GetOffering - Response", respBody);
 
        Allure.step("Offering details fetched successfully for MSISDN=" + MSISDN);
    }
    
    @Epic("Prepaid")
    @Feature("Topup History")
    @Story("Get Topup Balance History by MSISDN")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 15)
    void GetTopupHistory() {
 
        Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
 
      
        String url =
            "https://st.dbss-rm.oci-np.kpn.org/brm/prepayBalanceManagement/v4/topupBalance"
            + "?logicalResource.value=" + MSISDN
            + "&fields=id,href,amount,partyAccount,product,reason,receiver,receiverPartyAccount,receiverProduct,"
            + "relatedParty,requestedDate,requestor,status,transferCost,usageType,relatedTopupBalance,payment,impactedBucket,logicalResource";
 
        // Attach to Allure for visibility
        attachText("GetTopupHistory - Request URL", url);
        Allure.step("Fetching topup history for MSISDN=" + MSISDN);
 
        
        ValidatableResponse vr =
            given()
                .relaxedHTTPSValidation()
                .log().all()
            .when()
                .get(url)
            .then()
                .log().ifValidationFails()
              .statusCode(200)
                .log().all();
 
        String respBody = vr.extract().asString();
        attachJson("GetTopupHistory - Response", respBody);
        Allure.step("Topup history fetched successfully for MSISDN=" + MSISDN);
    }
    
    
    @Epic("Prepaid")
    @Feature("Account Hierarchy")
    @Story("Get Account Hierarchy by MSISDN ")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 16)
    void GetAccountHierarchy_MSISDN() {
 
        Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
        String msisdnToQuery = MSISDN;
 
        String url = "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getAccountHierarchy";
 
        String payload =
            "{\r\n" +
            "  \"operation\": \"SEARCH\",\r\n" +
            "  \"searchCriteria\": {\r\n" +
            "    \"entityType\": \"ServiceAccount\",\r\n" +
            "    \"filters\": {\r\n" +
            "      \"aliasList\": [\r\n" +
            "        {\r\n" +
            "          \"name\": \"MSISDN1\",\r\n" +
            "          \"value\": \"" + msisdnToQuery + "\"\r\n" +
            "        }\r\n" +
            "      ]\r\n" +
            "    }\r\n" +
            "  }\r\n" +
            "}";
 
        attachJson("GetAccountHierarchy (MSISDN only) - Request", payload);
        Allure.step("Fetching Account Hierarchy using MSISDN=" + msisdnToQuery);
        
        RequestSpecification spec = new RequestSpecBuilder()
        	    .setRelaxedHTTPSValidation()
        	    .setContentType("application/json")
        	    .addHeader("Accept", "*/*")
        	    .addHeader("User-Agent", "PostmanRuntime/7.43.0")
        	    .addHeader("Accept-Encoding", "gzip, deflate, br")
        	    .addHeader("Connection", "keep-alive")
        	    .build();
        RestAssured.config = RestAssured.config()
                .redirect(RedirectConfig.redirectConfig().followRedirects(true));
        
        
        
        ValidatableResponse vr = ApiRetry.run(
                "GetAccountHierarchy_MSISDN",
                () ->
                    given()
                        .spec(spec)
                        .body(payload)
                    .when()
                        .post(url)
                    .then()
                    	.log().ifValidationFails()
                        .statusCode(200)
                        .log().all()
        );
 
        String respBody = vr.extract().asString();
        attachJson("GetAccountHierarchy (MSISDN only) - Response", respBody);
        Allure.step("Account Hierarchy fetched successfully.");
    }

private BigDecimal fetchBadPinCounter(OracleDB db, Long accountObjId0) {

    String sql =
        "select c.current_bal as CURRENT_BAL " +
        "from bal_grp_sub_bals_t c, bal_grp_t bb, CONFIG_BEID_BALANCES_T cbd " +
        "where c.obj_id0 = bb.poid_id0 " +
        "and bb.account_obj_id0 = ? " +
        "and c.rec_id2 = 2000006 " +
        "and cbd.rec_id = c.rec_id2";

    String value = db.getSingleValue(sql, accountObjId0);

   
    return value == null ? BigDecimal.ZERO : new BigDecimal(value);
}

@Epic("Prepaid")
@Feature("BadPinCounter")
@Story("Increment Bad Pin Counter")
@Severity(SeverityLevel.NORMAL)
@Test(dependsOnMethods = {"CreateAccount"}, priority = 17)
public void BadPinCounter() {

    Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
    Assert.assertNotNull(lastAccountObjId0, "Account Obj ID not available");

    // ---------- DB CONNECTION ----------
    OracleDB db = new OracleDB(
        "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
        "pin",
        "BrmDb5#rm#11"
    );

    // ================= DB BEFORE =================
    BigDecimal before = fetchBadPinCounter(db, lastAccountObjId0);
    attachOraDbBalance("DB BadPinCounter BEFORE", before);
    Allure.step("BadPinCounter BEFORE API = " + before);

    // ---------- COOKIE ----------
    String cookie =
        "nginxingresscookie=0d70706d807ed83bd5d726961a21d4b0|8038bf2692513c815d85f28f02a6ef15";

    // ---------- PAYLOAD ----------
    String payload =
        "{\n" +
        "  \"requestDetails\": {\n" +
        "    \"description\": \"BadPinCounter\",\n" +
        "    \"logicalResource\": {\n" +
        "      \"id\": \"" + MSISDN + "\",\n" +
        "      \"name\": \"MSISDN1\"\n" +
        "    }\n" +
        "  }\n" +
        "}";

    attachJson("BadPin - Request", payload);
    Allure.step("Triggering BadPinCounter for MSISDN=" + MSISDN);

    // ---------- API CALL ----------
    RestAssured.config = RestAssured.config()
            .redirect(RedirectConfig.redirectConfig().followRedirects(true));

    ValidatableResponse vr =
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .accept("*/*")
            .header("User-Agent", "PostmanRuntime/7.43.0")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Connection", "keep-alive")
            .header("Cookie", cookie)
            .body(payload)
            .log().all()
        .when()
            .post("https://st.dbss-rm.oci-np.kpn.org/brm/extn/prepayBalanceManagement/v4/badPin")
        .then()
            .log().ifValidationFails()
            .statusCode(200);

    attachJson("BadPin - Response", vr.extract().asString());
    Allure.step("BadPinCounter API processed successfully");

    // ================= DB AFTER =================
    sleepSeconds(8); // allow DB commit

    BigDecimal after = fetchBadPinCounter(db, lastAccountObjId0);
    attachOraDbBalance("DB BadPinCounter AFTER", after);
    Allure.step("BadPinCounter AFTER API = " + after);

    // ================= ASSERTION =================
    BigDecimal expected = before.add(BigDecimal.ONE);

    Assert.assertEquals(
        after.compareTo(expected),
        0,
        "BadPinCounter value did not increment by 1"
    );

    Allure.step(
        "BadPinCounter DB validation successful: " +
        before + " → " + after
    );
	}
@Epic("Prepaid")
@Feature("Usage Management")
@Story("Get KPN Pay Usage by MSISDN")
@Severity(SeverityLevel.NORMAL)
@Test(dependsOnMethods = "CreateAccount", priority = 18)
public void GetKpnPay() {
    sleepSeconds(15);

    // ---------- PRE-CONDITIONS ----------
    Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
    String msisdnToQuery = MSISDN;

    // ---------- DATE PREPARATION ----------
    Instant now = Instant.now();
    String startDate = DateTimeFormatter.ISO_INSTANT.format(now.minus(1, ChronoUnit.DAYS)); // Yesterday
    String endDate = DateTimeFormatter.ISO_INSTANT.format(now.plus(1, ChronoUnit.DAYS));   // Tomorrow

    String baseUrl = "https://st.dbss-rm.oci-np.kpn.org/brm/extn/usageManagement/v4/getKpnPay";

    System.out.println("----- SENDING GET KPN PAY REQUEST -----");
    System.out.println("MSISDN: " + msisdnToQuery);
    System.out.println("Start Date: " + startDate);
    System.out.println("End Date: " + endDate);

    RequestSpecification spec = new RequestSpecBuilder()
            .setRelaxedHTTPSValidation()
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "PostmanRuntime/7.43.0")
            .build();

    // ---------- API CALL (GET) ----------
    ValidatableResponse vr = ApiRetry.run(
            "GetKpnPay",
            () ->
                given()
                    .spec(spec)
                    .queryParam("msisdn", msisdnToQuery)
                    .queryParam("startDate", startDate)
                    .queryParam("endDate", endDate)
                .when()
                    .get(baseUrl)
                .then()
                    .log().ifValidationFails()
                    .statusCode(200)
    );

    // ---------- LOGGING ----------
    String respBody = vr.extract().response().asPrettyString();
    attachJson("GetKpnPay - Response", respBody);

    System.out.println("----- GET KPN PAY RESPONSE START -----");
    System.out.println(respBody);
    System.out.println("----- GET KPN PAY RESPONSE END -----");

    // ---------- VERIFICATION ----------
    String rawRespBody = vr.extract().asString();
    if (!rawRespBody.equals("[]")) {
        String usageType = vr.extract().path("[0].usageType");
        Assert.assertEquals(
            usageType,
            "KPN_PAY",
            "Usage type mismatch in response! Expected KPN_PAY but found: " + usageType
        );
    }

    Allure.step("KPN Pay details fetched successfully for MSISDN: " + msisdnToQuery);
}

    
    
@Epic("Prepaid")
@Feature("COIN Management")
@Story("Activate COIN Event")
@Severity(SeverityLevel.CRITICAL)
@Test(priority = 19)
void coinnumberaddition() {
	sleepSeconds(15);

    // ================= 1. UNIQUE COIN NUMBER GENERATION =================
    // Uses "99" + last 7 digits of current timestamp to ensure uniqueness
    String timestamp = String.valueOf(System.currentTimeMillis());
    String COIN_NUMBER = "99" + timestamp.substring(timestamp.length() - 7); 

    // ================= 2. DATE PREPARATION =================
    Instant now = Instant.now();
    String plannedDate = DateTimeFormatter.ISO_INSTANT.format(now);
    String registrationDate = DateTimeFormatter.ISO_INSTANT.format(now);

    // ================= 3. PAYLOAD PREPARATION =================
    String coinActivatePayload = "{\r\n" +
            "  \"coinEventDetail\": {\r\n" +
            "    \"operatorCode\": \"ACT\",\r\n" +
            "    \"plannedDateTime\": \"" + plannedDate + "\",\r\n" +
            "    \"registrationDate\": \"" + registrationDate + "\",\r\n" +
            "    \"items\": [\r\n" +
            "      {\r\n" +
            "        \"servicePhoneNumber\": \"" + COIN_NUMBER + "\",\r\n" +
            "        \"pointOfPresence\": \"Y\",\r\n" +
            "        \"tariff\": {\r\n" +
            "          \"type\": \"1\",\r\n" +
            "          \"vat\": \"0\",\r\n" +
            "          \"peak\": {\r\n" +
            "            \"amount\": \"44.5\",\r\n" +
            "            \"currency\": \"EUR\"\r\n" +
            "          },\r\n" +
            "          \"offPeak\": {\r\n" +
            "            \"amount\": \"88.5\",\r\n" +
            "            \"currency\": \"EUR\"\r\n" +
            "          },\r\n" +
            "          \"setupFee\": {\r\n" +
            "            \"amount\": \"0.0951\",\r\n" +
            "            \"currency\": \"EUR\"\r\n" +
            "          }\r\n" +
            "        }\r\n" +
            "      }\r\n" +
            "    ]\r\n" + 
            "  }\r\n" +
            "}";

    // ================= 4. API CALL =================
    attachJson("ActivateCoin - Request", coinActivatePayload);

    RequestSpecification spec = new RequestSpecBuilder()
            .setRelaxedHTTPSValidation()
            .setContentType("application/json")
            .addHeader("Accept", "*/*")
            .addHeader("User-Agent", "PostmanRuntime/7.43.0")
            .build();

    System.out.println("----- SENDING ACTIVATE COIN REQUEST FOR: " + COIN_NUMBER + " -----");

    ValidatableResponse response = ApiRetry.run(
            "ActivateCoin",
            () ->
                given()
                    .spec(spec)
                    .body(coinActivatePayload)
                .when()
                    .post("https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/coin/activate")
                .then()
                    .statusCode(201) 
    );

    // ================= 5. GUARANTEED CONSOLE LOGGING =================
    String responseBody = response.extract().response().asPrettyString();
    
    System.out.println("----- ACTIVATE COIN RESPONSE START -----");
    System.out.println(responseBody);
    System.out.println("----- ACTIVATE COIN RESPONSE END -----");

    attachJson("ActivateCoin - Response", responseBody);
    Allure.step("COIN Activate API executed for COIN: " + COIN_NUMBER);
	}
    
    
    @Epic("Prepaid")
    @Feature("Account Management")
    @Story("Get Prepaid Account by MSISDN")
    @Severity(SeverityLevel.CRITICAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 20)
    void GetPrepaidAccount_MSISDN() {
    	sleepSeconds(15);

        Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
        String msisdnToQuery = MSISDN;

        String baseUrl = "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getPrepaidAccount";

        System.out.println("----- SENDING GET PREPAID ACCOUNT REQUEST FOR: " + msisdnToQuery + " -----");

        RequestSpecification spec = new RequestSpecBuilder()
                .setRelaxedHTTPSValidation()
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "PostmanRuntime/7.43.0")
                .addHeader("Connection", "keep-alive")
                .build();

        ValidatableResponse vr = ApiRetry.run(
                "GetPrepaidAccount_MSISDN",
                () ->
                    given()
                        .spec(spec)
                        .queryParam("msisdn", msisdnToQuery)
                    .when()
                        .get(baseUrl)
                    .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .log().all()
        );

        String respBody = vr.extract().asString();
        attachJson("GetPrepaidAccount - Response", respBody);
        Allure.step("Prepaid Account fetched successfully for MSISDN: " + msisdnToQuery);
    }
    
    @Epic("Prepaid")
    @Feature("Account Management")
    @Story("Get Profile Info by Service Account ID")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 21)
    public void GetProfileInfo() {
    	sleepSeconds(15);

        // ---------- PRE-CONDITIONS ----------
        Assert.assertNotNull(serviceAccountId, "Service Account ID not captured from CreateAccount");
        Assert.assertNotNull(lastAccountObjId0, "Account Obj ID not available");

        String accountIdToQuery = serviceAccountId;
        String url = "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getProfileInfo";

        // ---------- PAYLOAD ----------
        String payload = "{\n" +
                "  \"operation\": \"SEARCH\",\n" +
                "  \"searchCriteria\": {\n" +
                "    \"entityType\": \"ServiceAccount\",\n" +
                "    \"filters\": {\n" +
                "      \"accountId\": \"" + accountIdToQuery + "\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        attachJson("GetProfileInfo - Request", payload);
        System.out.println("----- SENDING GET PROFILE INFO REQUEST FOR: " + accountIdToQuery + " -----");

        RequestSpecification spec = new RequestSpecBuilder()
                .setRelaxedHTTPSValidation()
                .setContentType("application/json")
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "PostmanRuntime/7.43.0")
                .build();

        // ---------- API CALL ----------
        ValidatableResponse vr = ApiRetry.run(
                "GetProfileInfo",
                () ->
                    given()
                        .spec(spec)
                        .body(payload)
                    .when()
                        .post(url)
                    .then()
                        .log().ifValidationFails()
                        .statusCode(200)
                        .log().all()
        );

        // ---------- LOGGING ----------
        String respBody = vr.extract().asString();
        attachJson("GetProfileInfo - Response", respBody);
        
        System.out.println("----- GET PROFILE INFO RESPONSE START -----");
        System.out.println(respBody);
        System.out.println("----- GET PROFILE INFO RESPONSE END -----");

        Allure.step("Profile Info fetched successfully for Account ID: " + accountIdToQuery);
        
        // Optional: Log the Status from the response body to Allure
        String statusDescription = vr.extract().path("status.statusDescription");
        Allure.step("API Status Description: " + statusDescription);
    }
    
    @Epic("Prepaid")
    @Feature("Usage Management")
    @Story("Get Usage Details by MSISDN")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 22)
    public void GetUsagecdr() {
        sleepSeconds(20);

        // ---------- PRE-CONDITIONS ----------
        Assert.assertNotNull(MSISDN, "MSISDN not available from CreateAccount test");
        String msisdnToQuery = MSISDN;

        // ---------- DATE PREPARATION ----------
        Instant now = Instant.now();
        String startDate = DateTimeFormatter.ISO_INSTANT.format(now.minus(1, ChronoUnit.DAYS));
        String endDate = DateTimeFormatter.ISO_INSTANT.format(now.plus(1, ChronoUnit.DAYS));

        String baseUrl = "https://st.dbss-rm.oci-np.kpn.org/brm/extn/usageManagement/v4/getUsage";

        System.out.println("----- SENDING GET USAGE REQUEST -----");
        System.out.println("MSISDN: " + msisdnToQuery);
        System.out.println("Start Date (Yesterday): " + startDate);
        System.out.println("End Date (Tomorrow): " + endDate);

        RequestSpecification spec = new RequestSpecBuilder()
                .setRelaxedHTTPSValidation()
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "PostmanRuntime/7.43.0")
                .addHeader("Connection", "keep-alive")
                .build();

        // ---------- API CALL (GET) ----------
        ValidatableResponse vr = ApiRetry.run(
                "GetUsagecdr",
                () ->
                    given()
                        .spec(spec)
                        .queryParam("msisdn", msisdnToQuery)
                        .queryParam("type", "Voice") // Correct: use type=Voice
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                    .when()
                        .get(baseUrl)
                    .then()
                        .log().ifValidationFails()
                        .statusCode(200)
        );

        // ---------- LOGGING ----------
        String respBody = vr.extract().response().asPrettyString();
        attachJson("GetUsagecdr - Response", respBody);

        System.out.println("----- GET USAGE RESPONSE START -----");
        System.out.println(respBody);
        System.out.println("----- GET USAGE RESPONSE END -----");

        // ---------- VERIFICATION ----------
        String rawRespBody = vr.extract().asString();
        if (!rawRespBody.equals("[]")) {
            String usageType = vr.extract().path("[0].usageType");
            Assert.assertEquals(
                usageType,
                "recurring_daily",
                "Usage type mismatch in response! Expected recurring_daily but found: " + usageType
            );
        }

        Allure.step("Usage details fetched successfully for MSISDN: " + msisdnToQuery);
        Allure.step("Date Range: " + startDate + " to " + endDate);
    }

    @Epic("Prepaid")
    @Feature("Account Hierarchy")
    @Story("Get Billing Account Hierarchy")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 23)
    public void GetBillingAccount() {
        sleepSeconds(15);

        // ---------- PRE-CONDITIONS ----------
        Assert.assertNotNull(billingAccountId, "Billing Account ID not captured from CreateAccount");
        String baToQuery = billingAccountId;

        String url = "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/getAccountHierarchy";

        // ---------- PAYLOAD ----------
        String payload = "{\n" +
                "  \"operation\": \"SEARCH\",\n" +
                "  \"searchCriteria\": {\n" +
                "    \"entityType\": \"BillingAccount\",\n" +
                "    \"filters\": {\n" +
                "      \"accountId\": \"" + baToQuery + "\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        attachJson("GetBillingAccount - Request", payload);
        System.out.println("----- SENDING GET BILLING ACCOUNT REQUEST FOR: " + baToQuery + " -----");

        RequestSpecification spec = new RequestSpecBuilder()
                .setRelaxedHTTPSValidation()
                .setContentType("application/json")
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "PostmanRuntime/7.43.0")
                .build();

        // ---------- API CALL (POST) ----------
        ValidatableResponse vr = ApiRetry.run(
                "GetBillingAccount",
                () ->
                    given()
                        .spec(spec)
                        .body(payload)
                    .when()
                        .post(url)
                    .then()
                        .log().ifValidationFails()
                        .statusCode(200)
        );

        // ---------- LOGGING ----------
        String respBody = vr.extract().response().asPrettyString();
        attachJson("GetBillingAccount - Response", respBody);
        
        System.out.println("----- GET BILLING ACCOUNT RESPONSE START -----");
        System.out.println(respBody);
        System.out.println("----- GET BILLING ACCOUNT RESPONSE END -----");

        // Simple validation of the status object in the response
        String statusDescription = vr.extract().path("status.statusDescription");
        Allure.step("Billing Account status: " + statusDescription);
        
        Allure.step("Billing Account details fetched successfully for: " + baToQuery);
    }
    
    @Epic("Prepaid")
    @Feature("Account Management")
    @Story("Change Customer Account Address")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 24)
    public void ChangeAddressCustomer() {

        sleepSeconds(15);

        // ---------- PRE-CONDITIONS ----------
        Assert.assertNotNull(customerAccountId, "Customer Account ID not captured from CreateAccount");
        Assert.assertFalse(customerAccountId.isEmpty(), "Customer Account ID is empty");

        String caToQuery = customerAccountId;

        // ---------- DB CONNECTION ----------
        OracleDB db = new OracleDB(
            "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
            "pin",
            "BrmDb5#rm#11"
        );

        // ========== DB BEFORE ==========
        String beforeCountry = fetchCountry(db, customerAccountId);
        String beforeCompany = fetchCompany(db, customerAccountId);

        attachText("Before Country", beforeCountry);
        attachText("Before Company", beforeCompany);

        Allure.step("Before Country = " + beforeCountry);
        Allure.step("Before Company = " + beforeCompany);

        // ---------- API ----------
        String url = "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/changeAddress";

        String payload = "{\n" +
                "  \"operation\": \"UPDATE\",\n" +
                "  \"searchCriteria\": {\n" +
                "    \"entityType\": \"CustomerAccount\",\n" +
                "    \"filters\": {\n" +
                "      \"accountId\": \"" + caToQuery + "\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"contacts\": {\n" +
                "    \"firstName\": \"may11\",\n" +
                "    \"middleName\": \"\",\n" +
                "    \"lastName\": \"cust_acc_tc1\",\n" +
                "    \"company\": \"Oracle\",\n" +
                "    \"address\": \"1234 Main Street\",\n" +
                "    \"city\": \"Hometown\",\n" +
                "    \"state\": \"TX\",\n" +
                "    \"zip\": \"55555-4444\",\n" +
                "    \"country\": \"Germany\",\n" +
                "    \"houseNumber\": \"CA_111\",\n" +
                "    \"houseNumberExt\": \"CA_222\",\n" +
                "    \"street\": \"CA_333\"\n" +
                "  }\n" +
                "}";

        attachJson("ChangeAddressCustomer - Request", payload);
        System.out.println("----- SENDING CHANGE ADDRESS REQUEST FOR: " + caToQuery + " -----");

        RequestSpecification spec = new RequestSpecBuilder()
                .setRelaxedHTTPSValidation()
                .setContentType("application/json")
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "PostmanRuntime/7.43.0")
                .build();

        // ---------- API CALL ----------
        ValidatableResponse vr = ApiRetry.run(
                "ChangeAddressCustomer",
                () ->
                        given()
                                .spec(spec)
                                .body(payload)
                        .when()
                                .post(url)
                        .then()
                                .log().ifValidationFails()
                                .statusCode(200)
                                .log().all()
        );

        // ---------- RESPONSE ----------
        String respBody = vr.extract().asString();
        attachJson("ChangeAddressCustomer - Response", respBody);

        String statusDescription = vr.extract().path("statusDescription");

        Assert.assertNotNull(statusDescription, "Status description missing");
        Assert.assertTrue(
                statusDescription.toLowerCase().contains("success"),
                "Change Address failed: " + statusDescription
        );

        // ========== DB AFTER ==========
        sleepSeconds(20);

        String afterCountry = fetchCountry(db, customerAccountId);
        String afterCompany = fetchCompany(db, customerAccountId);

        attachText("After Country", afterCountry);
        attachText("After Company", afterCompany);

        Allure.step("After Country = " + afterCountry);
        Allure.step("After Company = " + afterCompany);

        // ---------- DEBUG ----------
        System.out.println("Before Country: " + beforeCountry);
        System.out.println("After Country: " + afterCountry);
        System.out.println("Before Company: " + beforeCompany);
        System.out.println("After Company: " + afterCompany);

        // ========== ASSERTIONS ==========
        Assert.assertEquals(afterCountry, "Germany", "Country not updated correctly");
        Assert.assertEquals(afterCompany, "Oracle", "Company not updated correctly");

        Allure.step("✅ Customer Account DB validation successful");
    }
    private String fetchCountry(OracleDB db, String customerAccountId) {

        String sql =
            "select ani.country " +
            "from account_nameinfo_t ani, account_t a " +
            "where a.account_no = '" + customerAccountId + "' " +
            "and ani.obj_id0 = a.poid_id0 " +
            "and ani.rec_id = (" +
            "    select max(rec_id) from account_nameinfo_t where obj_id0 = a.poid_id0" +
            ")";

        return db.getSingleValue(sql);
    }
    private String fetchCompany(OracleDB db, String customerAccountId) {

        String sql =
            "select ani.company " +
            "from account_nameinfo_t ani, account_t a " +
            "where a.account_no = '" + customerAccountId + "' " +
            "and ani.obj_id0 = a.poid_id0 " +
            "and ani.rec_id = (" +
            "    select max(rec_id) from account_nameinfo_t where obj_id0 = a.poid_id0" +
            ")";

        return db.getSingleValue(sql);
    }
    
    @Epic("Prepaid")
    @Feature("Account Management")
    @Story("Update Service Account Status")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 25)
    public void UpdateServiceStatus() {

        sleepSeconds(15);

        // ---------- PRE-CONDITIONS ----------
        Assert.assertNotNull(serviceAccountId, "Service Account ID not captured from CreateAccount");
        Assert.assertNotNull(MSISDN, "MSISDN global variable not initialized from CreateAccount");
        Assert.assertNotNull(lastAccountObjId0, "Service Account Obj ID (lastAccountObjId0) not available");

        String saToQuery = serviceAccountId;
        
        // Extract raw epoch number from MSISDN
        String rawEpochMsisdn = MSISDN.substring(2); 
        String targetedServiceKey = "service-id" + rawEpochMsisdn;

        // ---------- DB CONNECTION ----------
        OracleDB db = new OracleDB(
            "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
            "pin",
            "BrmDb5#rm#11"
        );

        // ========== DB BEFORE ==========
        String beforeState = fetchLifecycleState(db, lastAccountObjId0);
        attachText("Before Lifecycle State", beforeState);
        Allure.step("Before Lifecycle State = " + beforeState);

        // ---------- API ----------
        String url = "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/updateStatus";

        String payload = "{\n" +
                "  \"operation\": \"UPDATE\",\n" +
                "  \"searchCriteria\": {\n" +
                "    \"entityType\": \"ServiceAccount\",\n" +
                "    \"filters\": {\n" +
                "      \"accountId\": \"" + saToQuery + "\",\n" +
                "      \"serviceKey\": \"" + targetedServiceKey + "\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"partyAccount\": {\n" +
                "    \"@type\": \"ServiceAccount\",\n" +
                "    \"lifecycleStatus\": \"ACTIVE\"\n" +
                "  }\n" +
                "}";

        attachJson("UpdateStatus - Request", payload);
        System.out.println("----- SENDING UPDATE STATUS REQUEST FOR SA: " + saToQuery + " WITH KEY: " + targetedServiceKey + " -----");

        RequestSpecification spec = new RequestSpecBuilder()
                .setRelaxedHTTPSValidation()
                .setContentType("application/json")
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "PostmanRuntime/7.43.0")
                .build();

        // ---------- API CALL ----------
        ValidatableResponse vr = ApiRetry.run(
                "UpdateStatus",
                () ->
                    given()
                        .spec(spec)
                        .body(payload)
                    .when()
                        .post(url)
                    .then()
                        .log().ifValidationFails()
                        .statusCode(200)
        );

        // ---------- RESPONSE ----------
        String respBody = vr.extract().response().asPrettyString();
        attachJson("UpdateStatus - Response", respBody);
        
        System.out.println("----- UPDATE STATUS RESPONSE START -----");
        System.out.println(respBody);
        System.out.println("----- UPDATE STATUS RESPONSE END -----");

        String statusDescription = vr.extract().path("statusDescription");
        Assert.assertNotNull(statusDescription, "Status description missing");
        Assert.assertTrue(
                statusDescription.toLowerCase().contains("success"),
                "Update Status failed: " + statusDescription
        );

        // ========== DB AFTER ==========
        sleepSeconds(20);

        String afterState = fetchLifecycleState(db, lastAccountObjId0);
        attachText("After Lifecycle State", afterState);
        Allure.step("After Lifecycle State = " + afterState);

        // ---------- DEBUG ----------
        System.out.println("Before Lifecycle State: " + beforeState);
        System.out.println("After Lifecycle State: " + afterState);

        // ========== ASSERTIONS ==========
        Assert.assertNotEquals(beforeState, afterState, "Lifecycle state did not modify or transition inside the database!");
        
        Allure.step("✅ Service Account Lifecycle State DB validation successful");
    }

    private String fetchLifecycleState(OracleDB db, Long serviceAccountId0) {
        String sql = "select s.lifecycle_state " +
                     "from service_t s, SERVICE_ALIAS_LIST_T st " +
                     "where s.poid_id0 = st.obj_id0 " +
                     "and s.account_obj_id0 = " + serviceAccountId0 + " " +
                     "and rownum = 1";
        return db.getSingleValue(sql);
    }
    
    @Epic("Prepaid")
    @Feature("Account Management")
    @Story("Update Service Account Status")
    @Severity(SeverityLevel.NORMAL)
    @Test(dependsOnMethods = "CreateAccount", priority = 26)
    public void UpdateAccountStatus() {

        sleepSeconds(15);

        // ---------- PRE-CONDITIONS ----------
        Assert.assertNotNull(serviceAccountId, "Service Account ID not captured from CreateAccount");
        Assert.assertNotNull(lastAccountObjId0, "Service Account Obj ID (lastAccountObjId0) not available");

        String saToQuery = serviceAccountId;

        // ---------- DB CONNECTION ----------
        OracleDB db = new OracleDB(
            "jdbc:oracle:thin:@//localhost:1011/brmstpdb.dbsubnet.devtestvcn.oraclevcn.com",
            "pin",
            "BrmDb5#rm#11"
        );

        // ========== DB BEFORE ==========
        String beforeAccountStatus = fetchStatus(db, lastAccountObjId0);
        attachText("Before Account Status", beforeAccountStatus);
        Allure.step("Before Account Status = " + beforeAccountStatus);

        // ---------- API ----------
        String url = "https://st.dbss-rm.oci-np.kpn.org/bcws/webresources/v1.0/custom/updateStatus";

        String payload = "{\n" +
                "  \"operation\": \"UPDATE\",\n" +
                "  \"searchCriteria\": {\n" +
                "    \"entityType\": \"ServiceAccount\",\n" +
                "    \"filters\": {\n" +
                "      \"accountId\": \"" + saToQuery + "\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"partyAccount\": {\n" +
                "    \"@type\": \"ServiceAccount\",\n" +
                "    \"status\": \"IN_ACTIVE\"\n" +
                "  }\n" +
                "}";

        attachJson("UpdateAccountStatus - Request", payload);
        System.out.println("----- SENDING UPDATE STATUS REQUEST FOR SA: " + saToQuery + " -----");

        RequestSpecification spec = new RequestSpecBuilder()
                .setRelaxedHTTPSValidation()
                .setContentType("application/json")
                .addHeader("Accept", "*/*")
                .addHeader("User-Agent", "PostmanRuntime/7.43.0")
                .build();

        // ---------- API CALL ----------
        ValidatableResponse vr = ApiRetry.run(
                "UpdateAccountStatus",
                () ->
                    given()
                        .spec(spec)
                        .body(payload)
                    .when()
                        .post(url)
                    .then()
                        .log().ifValidationFails()
                        .statusCode(200)
        );

        // ---------- RESPONSE ----------
        String respBody = vr.extract().response().asPrettyString();
        attachJson("UpdateAccountStatus - Response", respBody);
        
        System.out.println("----- UPDATE STATUS RESPONSE START -----");
        System.out.println(respBody);
        System.out.println("----- UPDATE STATUS RESPONSE END -----");

        String statusDescription = vr.extract().path("statusDescription");
        Assert.assertNotNull(statusDescription, "Status description missing");
        Assert.assertTrue(
                statusDescription.toLowerCase().contains("success"),
                "Update Status failed: " + statusDescription
        );

        // ========== DB AFTER ==========
        sleepSeconds(20);

        String afterAccountStatus = fetchStatus(db, lastAccountObjId0);
        attachText("After Account Status", afterAccountStatus);
        Allure.step("After Account Status = " + afterAccountStatus);

        // ---------- DEBUG ----------
        System.out.println("Before Account Status: " + beforeAccountStatus);
        System.out.println("After Account Status: " + afterAccountStatus);

        // ========== ASSERTIONS ==========
        Assert.assertNotEquals(beforeAccountStatus, afterAccountStatus, "Status code did not modify or transition inside the database!");
        
        Allure.step("✅ Service Account Status DB validation successful");
    }

    private String fetchStatus(OracleDB db, Long serviceAccountId0) {
        String sql = "select s.status " +
                     "from service_t s, SERVICE_ALIAS_LIST_T st " +
                     "where s.poid_id0 = st.obj_id0 " +
                     "and s.account_obj_id0 = " + serviceAccountId0 + " " +
                     "and rownum = 1";
        return db.getSingleValue(sql);
    }
    
    
}
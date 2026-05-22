package KPNAutomation;

import io.restassured.response.ValidatableResponse;
import org.testng.Assert;

import java.util.function.Supplier;

public class ApiRetry {
	
	private static final int DEFAULT_ATTEMPTS = 3;
    private static final int DEFAULT_WAIT_SEC = 5;

    public static ValidatableResponse run(String apiName,
                                          Supplier<ValidatableResponse> request) {
        return run(apiName, DEFAULT_ATTEMPTS, DEFAULT_WAIT_SEC, request);
    }

    public static ValidatableResponse run(String apiName,
                                          int attempts,
                                          int waitSec,
                                          Supplier<ValidatableResponse> request) {

        Throwable last = null;

        for (int i = 1; i <= attempts; i++) {
            try {
                System.out.println("Retrying " + apiName + " | attempt " + i);
                return request.get();
            }
            catch (Throwable t) {
                last = t;
                System.out.println("All retries " + apiName + " failed: " + t.getMessage());

                try {
                    Thread.sleep(waitSec * 1000L);
                } catch (InterruptedException ignored) {}
            }
        }

        Assert.fail("API failed after retries -> " + apiName, last);
        return null;
    }

}

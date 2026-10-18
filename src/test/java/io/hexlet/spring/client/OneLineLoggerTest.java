package io.hexlet.spring.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Util;

/** OneLineLogger (LR6): every record is one line with configKey, method, URL and status. */
@ExtendWith(OutputCaptureExtension.class)
public class OneLineLoggerTest {

    private static final String CONFIG_KEY = "OrderClient#getOrdersByUserId(String)";

    private final OneLineLogger logger = new OneLineLogger();

    @Test
    public void testFormatLineJoinsConfigKeyAndMessage() {
        var line = OneLineLogger.formatLine(CONFIG_KEY, "---> %s %s", "GET", "http://localhost/orders/1");
        assertThat(line).isEqualTo(CONFIG_KEY + " - ---> GET http://localhost/orders/1");
    }

    @Test
    public void testFormatLineRemovesLineBreaks() {
        var line = OneLineLogger.formatLine(CONFIG_KEY, "%s", "[{\"id\":1},\n{\"id\":2}]\r\n");
        assertThat(line).doesNotContain("\n").doesNotContain("\r")
                .isEqualTo(CONFIG_KEY + " - [{\"id\":1}, {\"id\":2}]");
    }

    @Test
    public void testLogWritesSingleLine(CapturedOutput output) {
        logger.log(CONFIG_KEY, "<--- HTTP/1.1 %s (%sms)%n%s", 200, 12, "body");

        var lines = output.getOut().lines().filter(l -> l.contains(CONFIG_KEY)).toList();
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).contains(CONFIG_KEY + " - <--- HTTP/1.1 200 (12ms) body");
    }

    @Test
    public void testResponseIsLoggedInOneLineAndRebuffered(CapturedOutput output) throws Exception {
        var request = Request.create(Request.HttpMethod.GET, "http://localhost:8081/order-service/orders/1",
                Map.of("X-Request-Source", List.of("lab6-user-service")), null, StandardCharsets.UTF_8, null);
        var body = "[{\"id\":1,\n\"status\":\"NEW\"}]";
        var response = Response.builder()
                .status(200).reason("OK").request(request).headers(Map.of())
                .body(body, StandardCharsets.UTF_8)
                .build();

        var rebuffered = logger.logAndRebufferResponse(CONFIG_KEY, Logger.Level.FULL, response, 15);

        var lines = output.getOut().lines().filter(l -> l.contains(CONFIG_KEY)).toList();
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0))
                .contains(CONFIG_KEY + " - GET http://localhost:8081/order-service/orders/1 -> 200 (15 ms)")
                .contains("headers={X-Request-Source=[lab6-user-service]}")
                .contains("body=[{\"id\":1, \"status\":\"NEW\"}]");
        // the body can still be read by the decoder
        assertThat(Util.toString(rebuffered.body().asReader(StandardCharsets.UTF_8))).isEqualTo(body);
    }

    @Test
    public void testBasicLevelHasNoBody(CapturedOutput output) throws Exception {
        var request = Request.create(Request.HttpMethod.GET, "http://localhost/orders?status=NEW",
                Map.of(), null, StandardCharsets.UTF_8, null);
        var response = Response.builder().status(404).request(request).headers(Map.of())
                .body("missing", StandardCharsets.UTF_8).build();

        logger.logAndRebufferResponse(CONFIG_KEY, Logger.Level.BASIC, response, 3);

        assertThat(output.getOut())
                .contains(CONFIG_KEY + " - GET http://localhost/orders?status=NEW -> 404 (3 ms)")
                .doesNotContain("body=missing");
    }
}

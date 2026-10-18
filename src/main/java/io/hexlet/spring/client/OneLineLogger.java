package io.hexlet.spring.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import feign.Request;
import feign.Response;
import feign.Util;

/**
 * Custom Feign logger (LR6, "Simplifying Feign client logging"): every log record is a single line.
 *
 * <p>{@link #log(String, String, Object...)} is overridden as in the methodology: the record is built as
 * {@code configKey + " - " + format}; line breaks of bodies/headers are replaced with spaces.
 * In addition, the request/response logging is overridden so that one HTTP exchange gives one line
 * with the basic information: HTTP method, URL, response status, elapsed time (and, for the levels
 * {@code HEADERS}/{@code FULL}, request headers and the response body). Example:
 * <pre>
 * OrderClient#getOrdersByUserId(String) - GET http://localhost:8081/order-service/orders/1 -> 200 (15 ms)
 *     headers={X-Request-Source=[lab6-user-service]} body=[{"id":1,...}]
 * </pre>
 * Records are written through SLF4J (logger {@code io.hexlet.spring.client.OneLineLogger}, level INFO)
 * instead of {@code System.out.println} from the example, so they get into the application log.
 */
public class OneLineLogger extends feign.Logger {

    /** Maximum length of a response body in a record, so that the line stays readable. */
    static final int MAX_BODY_LENGTH = 300;

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(OneLineLogger.class);

    @Override
    protected void log(String configKey, String format, Object... args) {
        LOG.info(formatLine(configKey, format, args));
    }

    /** Builds a single-line record: {@code configKey + " - " + format} with line breaks replaced. */
    static String formatLine(String configKey, String format, Object... args) {
        var message = configKey + " - " + String.format(format, args);
        return message.replaceAll("[\\r\\n]+", " ").trim();
    }

    /** The request is logged together with the response (one line per exchange). */
    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        // nothing: the method and URL are written by logAndRebufferResponse / logIOException
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response,
                                              long elapsedTime) throws IOException {
        var request = response.request();
        var line = new StringBuilder()
                .append(request.httpMethod()).append(' ').append(request.url())
                .append(" -> ").append(response.status())
                .append(" (").append(elapsedTime).append(" ms)");

        Response result = response;
        if (logLevel.ordinal() >= Level.HEADERS.ordinal()) {
            line.append(" headers=").append(headers(request.headers()));
        }
        if (logLevel.ordinal() >= Level.FULL.ordinal() && response.body() != null) {
            byte[] body = Util.toByteArray(response.body().asInputStream());
            line.append(" body=").append(abbreviate(new String(body, StandardCharsets.UTF_8)));
            result = response.toBuilder().body(body).build(); // the body stream was consumed
        }
        log(configKey, "%s", line);
        return result;
    }

    @Override
    protected IOException logIOException(String configKey, Level logLevel, IOException ioe, long elapsedTime) {
        log(configKey, "%s", "ERROR " + ioe.getClass().getSimpleName() + ": " + ioe.getMessage()
                + " (" + elapsedTime + " ms)");
        return ioe;
    }

    @Override
    protected void logRetry(String configKey, Level logLevel) {
        log(configKey, "%s", "RETRYING");
    }

    private static String headers(Map<String, Collection<String>> headers) {
        return headers.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String abbreviate(String text) {
        return text.length() <= MAX_BODY_LENGTH ? text : text.substring(0, MAX_BODY_LENGTH) + "...";
    }
}

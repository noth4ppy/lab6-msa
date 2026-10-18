package io.hexlet.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import io.hexlet.spring.client.OneLineLogger;

/**
 * Configuration of the Feign client {@link io.hexlet.spring.client.OrderClient} (LR6), applied through
 * {@code @FeignClient(configuration = FeignClientConfig.class)}.
 *
 * <p>The class is marked {@code @Configuration} as in the methodology and lies in the scanned package,
 * so its beans are also the defaults for all Feign clients of the application (there is one client -
 * {@code OrderClient}). Timeouts are set per client in {@code application.properties}
 * ({@code spring.cloud.openfeign.client.config.order-service.*}).
 */
@Configuration
public class FeignClientConfig {

    /** Value of the header {@code X-Request-Source} added to every Feign request. */
    public static final String REQUEST_SOURCE = "lab6-user-service";

    /** Log level: FULL - method, URL, headers, body and metadata of requests and responses. */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /** Custom one-line logger instead of the standard multi-line Slf4jLogger. */
    @Bean
    public Logger logger() {
        return new OneLineLogger();
    }

    /**
     * Retries of failed requests (connection errors, {@code RetryableException}): first pause 100 ms,
     * maximum pause 1 s, at most 3 attempts in total. By default Spring Cloud OpenFeign does not retry.
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }

    /** Request interceptor: marks requests with the name of the calling service. */
    @Bean
    public RequestInterceptor requestSourceInterceptor() {
        return template -> template.header("X-Request-Source", REQUEST_SOURCE);
    }
}

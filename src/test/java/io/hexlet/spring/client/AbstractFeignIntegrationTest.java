package io.hexlet.spring.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.hexlet.spring.config.TestConfig;

/**
 * Common context of the Feign integration tests (LR6): a real embedded server is started, and the Feign
 * client OrderClient calls the Order-Service stub of the same application over HTTP.
 *
 * <p>The {@code url} of {@code @FeignClient} is resolved when the context starts, before a random port
 * ({@code RANDOM_PORT}) is known, therefore a free port is chosen in advance and used both as
 * {@code server.port} ({@code DEFINED_PORT}) and in {@code clients.order-service.url}. All subclasses share
 * one cached context (the same {@link DynamicPropertySource} method).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@Import(TestConfig.class)
public abstract class AbstractFeignIntegrationTest {

    protected static final int PORT = findFreePort();

    @DynamicPropertySource
    static void feignProperties(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> PORT);
        registry.add("clients.order-service.url", () -> "http://localhost:" + PORT + "/order-service");
    }

    private static int findFreePort() {
        try (var socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

package io.hexlet.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point of the book catalog.
 *
 * <p>LR6: {@link EnableFeignClients} makes Spring scan the base package {@code io.hexlet.spring}
 * for interfaces annotated with {@code @FeignClient} (see {@link io.hexlet.spring.client.OrderClient})
 * and create proxy implementations for them.
 */
@SpringBootApplication
@EnableFeignClients
public class Lab1Application {

    public static void main(String[] args) {
        SpringApplication.run(Lab1Application.class, args);
    }
}

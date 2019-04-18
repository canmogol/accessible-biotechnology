package dev.canm.ab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Boot entry point.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class SpringBootABApplication {

    /**
     * Spring Boot main method.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(SpringBootABApplication.class, args);
    }

}

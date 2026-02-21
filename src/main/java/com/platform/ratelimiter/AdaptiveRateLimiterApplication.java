package com.platform.ratelimiter;

import com.platform.ratelimiter.config.RateLimiterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RateLimiterProperties.class)
public class AdaptiveRateLimiterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdaptiveRateLimiterApplication.class, args);
    }
}

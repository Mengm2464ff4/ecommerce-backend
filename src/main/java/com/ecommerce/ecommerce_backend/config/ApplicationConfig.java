package com.ecommerce.ecommerce_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableWebMvc
public class ApplicationConfig {

    @Bean
    public RestTemplate restTemplate () {
        return new RestTemplate();
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("UTC"));
    }
}


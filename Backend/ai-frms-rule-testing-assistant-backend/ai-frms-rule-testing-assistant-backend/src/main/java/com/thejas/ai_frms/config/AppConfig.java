package com.thejas.ai_frms.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "com.thejas.ai_frms")
public class AppConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public DateTimeFormatter applicationDateTimeFormatter() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    }
}
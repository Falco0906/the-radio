package com.theradio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TheRadioApplication {
    public static void main(String[] args) {
        SpringApplication.run(TheRadioApplication.class, args);
    }
}


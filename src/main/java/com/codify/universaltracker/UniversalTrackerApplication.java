package com.codify.universaltracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class UniversalTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniversalTrackerApplication.class, args);
    }

}

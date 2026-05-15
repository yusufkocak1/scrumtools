package com.scrumtools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ScrumToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScrumToolsApplication.class, args);
    }
}


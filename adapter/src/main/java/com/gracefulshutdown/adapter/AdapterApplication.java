package com.gracefulshutdown.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdapterApplication {

    public static void main(String[] args) {
        System.out.println("Adapter version: 0.0.2");
        SpringApplication.run(AdapterApplication.class, args);
    }

}

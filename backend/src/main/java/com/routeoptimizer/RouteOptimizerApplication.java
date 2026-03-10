package com.routeoptimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RouteOptimizerApplication {

    public static void main(String[] args) {
        System.out.println("Starting Traffic Route Optimizer Backend...");
        SpringApplication.run(RouteOptimizerApplication.class, args);
    }

}

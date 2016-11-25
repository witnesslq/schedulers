package com.schedulers.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
//@ComponentScan(basePackages = {"com.schedulers"})
@ImportResource({"classpath:spring/application-*.xml"})
public class ApplicationStart {

    @RequestMapping("/")
    String index() {
        return "Hello Spring Boot";
    }

    public static void main(String[] args) {
        SpringApplication.run(ApplicationStart.class, args);
    }

}
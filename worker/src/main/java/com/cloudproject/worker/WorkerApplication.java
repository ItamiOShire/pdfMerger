package com.cloudproject.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class WorkerApplication {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(WorkerApplication.class, args);

        // Start Pub/Sub listener
        PubSubListener listener = context.getBean(PubSubListener.class);
        listener.start();
    }
}
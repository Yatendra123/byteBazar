package com.byteBazar.demo;

import com.byteBazar.ByteBazarApplication;
import org.springframework.boot.SpringApplication;

public class TestDemoApplication {

    public static void main(String[] args) {
        SpringApplication.from(ByteBazarApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

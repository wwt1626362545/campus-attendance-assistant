package com.educheck;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.educheck.mapper")
public class EduCheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduCheckApplication.class, args);
    }
}

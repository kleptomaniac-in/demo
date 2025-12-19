package com.example.pdf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.pdf", 
    "com.example.service", 
    "com.example.generator", 
    "com.example.function",
    "com.example.preprocessing",
    "com.example.admin",
    "com.example.config",
    "com.example.util"
})
public class PdfGenerationApplication {
    public static void main(String[] args) {
        SpringApplication.run(PdfGenerationApplication.class, args);
    }
}

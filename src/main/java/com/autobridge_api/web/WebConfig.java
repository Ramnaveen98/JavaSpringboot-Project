package com.autobridge_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${autobridge.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path p = Path.of(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + p.toString() + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}

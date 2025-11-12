package com.autobridge_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Profile;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Profile("local")
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    // Prod default = Cloud Run GCS FUSE mount; dev can override with "uploads"
    @Value("${autobridge.upload-dir:/mnt/gcs/uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /uploads/** → <uploadDir>/
      //  String root = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
       // if (!root.endsWith("/")) root = root + "/";
       // registry.addResourceHandler("/uploads/**")
         //       .addResourceLocations(root)
        //        .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
    }
}

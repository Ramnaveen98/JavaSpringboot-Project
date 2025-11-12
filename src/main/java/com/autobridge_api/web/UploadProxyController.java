package com.autobridge_api.web;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/uploads")
public class UploadProxyController {

    private final Storage storage;

    public UploadProxyController(Storage storage) {
        this.storage = storage;
    }

    @Value("${app.gcs.bucket:autobridge-uploads}")
    private String bucket;

    @GetMapping("/**")
    public ResponseEntity<byte[]> get(HttpServletRequest req) {
        // Extract the tail after /uploads/**
        String full = (String) req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String pattern = (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String tail = new AntPathMatcher().extractPathWithinPattern(pattern, full); // e.g. vehicles/22/xxx.png

        String objectName = "uploads/" + tail;

        Blob blob = storage.get(BlobId.of(bucket, objectName));
        if (blob == null || !blob.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        String ct = blob.getContentType();
        if (ct != null && !ct.isBlank()) {
            try { mt = MediaType.parseMediaType(ct); } catch (Exception ignored) {}
        }

        // Buffer the whole file (safe for typical image sizes)
        byte[] data = blob.getContent(); // downloads object into memory

        return ResponseEntity.ok()
                .contentType(mt)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(data);
    }
}

package com.autobridge_api.web;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/uploads")
public class UploadProxyController {

    private final Storage storage;
    public UploadProxyController(Storage storage) { this.storage = storage; }

    @Value("${app.gcs.bucket:autobridge-uploads}")
    private String bucket;

    @GetMapping("/**")
    public ResponseEntity<Void> redirect(HttpServletRequest req) {
        // Extract tail after /uploads/**
        String full = (String) req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String pattern = (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String tail = new AntPathMatcher().extractPathWithinPattern(pattern, full); // e.g. vehicles/22/xxx.png

        String objectName = "uploads/" + tail;
        BlobId id = BlobId.of(bucket, objectName);
        Blob blob = storage.get(id);
        if (blob == null || !blob.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Short-lived signed URL (10 minutes)
        BlobInfo info = BlobInfo.newBuilder(id).setContentType(blob.getContentType()).build();
        URL signed = storage.signUrl(
                info,
                10, TimeUnit.MINUTES,
                SignUrlOption.withV4Signature()
        );

        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .location(URI.create(signed.toString()))
                .build();
    }
}

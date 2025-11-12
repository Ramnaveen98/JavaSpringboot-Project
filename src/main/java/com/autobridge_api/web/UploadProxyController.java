package com.autobridge_api.web;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/uploads")
public class UploadProxyController {
    private final Storage storage;
    public UploadProxyController(Storage storage) { this.storage = storage; }

    @Value("${app.gcs.bucket:autobridge-uploads}")
    private String bucket;

    @GetMapping("/**")
    public ResponseEntity<Resource> get(HttpServletRequest req) {
        String full = (String) req.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String pattern = (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String tail = new AntPathMatcher().extractPathWithinPattern(pattern, full); // vehicles/22/xxx.png
        String objectName = "uploads/" + tail;

        Blob blob = storage.get(BlobId.of(bucket, objectName));
        if (blob == null || !blob.exists()) return ResponseEntity.notFound().build();

        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        String ct = blob.getContentType();
        if (ct != null && !ct.isBlank()) try { mt = MediaType.parseMediaType(ct); } catch (Exception ignored) {}

        try (ReadChannel reader = blob.reader()) {
            InputStream in = Channels.newInputStream(reader);
            InputStreamResource body = new InputStreamResource(in);
            return ResponseEntity.ok()
                    .contentType(mt)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(blob.getSize()))
                    .body(body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

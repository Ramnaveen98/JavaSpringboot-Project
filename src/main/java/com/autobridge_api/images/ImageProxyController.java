package com.autobridge_api.images;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/public/image-proxy")
public class ImageProxyController {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @GetMapping
    public ResponseEntity<byte[]> proxy(@RequestParam("url") String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Autobridge Image Proxy")
                    .build();
            HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() >= 400) return ResponseEntity.status(res.statusCode()).build();
            String ctype = res.headers().firstValue("content-type").orElse("application/octet-stream");
            if (!ctype.startsWith("image/")) return ResponseEntity.status(415).build();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(ctype))
                    .cacheControl(CacheControl.noCache())
                    .body(res.body());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

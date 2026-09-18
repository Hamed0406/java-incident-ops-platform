package com.networkincident;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health/live")
    Map<String, String> live() {
        return Map.of("status", "UP");
    }

    @GetMapping("/health/ready")
    Map<String, String> ready() {
        return Map.of("status", "UP");
    }
}

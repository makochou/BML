package com.bml.core.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "bml.web.cors")
public class CorsProperties {

    private List<String> allowedOriginPatterns = new ArrayList<>();

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns == null ? new ArrayList<>() : allowedOriginPatterns;
    }
}

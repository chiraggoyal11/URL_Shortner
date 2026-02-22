package com.urlshortener.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UrlValidationService {

    private static final Set<String> BLACKLISTED_DOMAINS = Set.of(
            "malicious-site.com",
            "phishing-site.com",
            "spam-site.com"
            // Add more as needed
    );

    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
            "^(\\d{1,3}\\.){3}\\d{1,3}$"
    );

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    /**
     * Validate URL for security and format
     */
    public void validateUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        if (urlString.length() > 2048) {
            throw new IllegalArgumentException("URL is too long (max 2048 characters)");
        }

        try {
            URI uri = URI.create(urlString);
            URL url = uri.toURL();

            // Check scheme
            String scheme = url.getProtocol();
            if (!ALLOWED_SCHEMES.contains(scheme)) {
                throw new IllegalArgumentException("Only HTTP and HTTPS URLs are allowed");
            }

            // Get host
            String host = url.getHost();
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid URL: missing host");
            }

            // Check for localhost
            if (isLocalhost(host)) {
                throw new IllegalArgumentException("Localhost URLs are not allowed");
            }

            // Check for IP addresses (optional - can disable if needed)
            if (IP_ADDRESS_PATTERN.matcher(host).matches()) {
                log.warn("URL with IP address detected: {}", host);
                // Uncomment to block IP addresses:
                // throw new IllegalArgumentException("IP address URLs are not allowed");
            }

            // Check blacklist
            if (isBlacklisted(host)) {
                log.warn("Blacklisted domain detected: {}", host);
                throw new IllegalArgumentException("This domain is not allowed");
            }

            // Check for open redirect attempts
            if (containsSuspiciousPatterns(urlString)) {
                throw new IllegalArgumentException("URL contains suspicious patterns");
            }

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error validating URL: " + e.getMessage());
        }
    }

    private boolean isLocalhost(String host) {
        return host.equals("localhost") ||
               host.equals("127.0.0.1") ||
               host.equals("0.0.0.0") ||
               host.equals("::1") ||
               host.endsWith(".local");
    }

    private boolean isBlacklisted(String host) {
        String lowerHost = host.toLowerCase();
        return BLACKLISTED_DOMAINS.stream()
                .anyMatch(blacklisted -> 
                    lowerHost.equals(blacklisted) || lowerHost.endsWith("." + blacklisted));
    }

    private boolean containsSuspiciousPatterns(String url) {
        String lowerUrl = url.toLowerCase();
        
        // Check for multiple redirects
        if (lowerUrl.contains("redirect") && lowerUrl.contains("http")) {
            return true;
        }
        
        // Check for javascript protocol
        if (lowerUrl.startsWith("javascript:") || lowerUrl.contains("javascript:")) {
            return true;
        }
        
        // Check for data protocol
        if (lowerUrl.startsWith("data:")) {
            return true;
        }
        
        return false;
    }
}

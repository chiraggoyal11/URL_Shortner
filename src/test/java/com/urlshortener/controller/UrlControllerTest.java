package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlService urlService;

    @Test
    void testCreateShortUrl_Success() throws Exception {
        // Arrange
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .build();

        UrlResponse response = UrlResponse.builder()
                .id(1L)
                .originalUrl("https://example.com")
                .shortCode("abc123")
                .shortUrl("http://localhost:8080/abc123")
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"));
    }

    @Test
    void testCreateShortUrl_InvalidUrl() throws Exception {
        // Arrange
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("invalid-url")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUrlStats_Success() throws Exception {
        // Arrange
        UrlResponse response = UrlResponse.builder()
                .id(1L)
                .originalUrl("https://example.com")
                .shortCode("abc123")
                .shortUrl("http://localhost:8080/abc123")
                .clickCount(42L)
                .createdAt(LocalDateTime.now())
                .build();

        when(urlService.getUrlStats("abc123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/urls/abc123/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.clickCount").value(42));
    }
}

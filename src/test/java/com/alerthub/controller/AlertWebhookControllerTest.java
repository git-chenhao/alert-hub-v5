package com.alerthub.controller;

import com.alerthub.dto.AlertRequest;
import com.alerthub.entity.Alert;
import com.alerthub.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AlertWebhookController 集成测试
 */
@WebMvcTest(AlertWebhookController.class)
class AlertWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    private AlertRequest testRequest;
    private Alert testAlert;

    @BeforeEach
    void setUp() {
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        testRequest = new AlertRequest();
        testRequest.setSource("prometheus");
        testRequest.setTitle("High CPU Usage");
        testRequest.setContent("CPU usage > 90%");
        testRequest.setSeverity("critical");
        testRequest.setLabels(labels);

        testAlert = Alert.builder()
            .id(1L)
            .fingerprint("test-fingerprint")
            .source("prometheus")
            .title("High CPU Usage")
            .content("CPU usage > 90%")
            .severity("critical")
            .status("pending")
            .build();
    }

    @Test
    void testReceiveAlert_Success() throws Exception {
        // Given
        when(alertService.receiveAlert(any(AlertRequest.class))).thenReturn(testAlert);

        // When & Then
        mockMvc.perform(post("/api/v1/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("告警已接收"))
            .andExpect(jsonPath("$.data.title").value("High CPU Usage"));
    }

    @Test
    void testReceiveAlert_Duplicate() throws Exception {
        // Given
        when(alertService.receiveAlert(any(AlertRequest.class))).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/v1/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("告警重复，已去重"));
    }

    @Test
    void testHealthCheck() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/alerts/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value("OK"));
    }
}

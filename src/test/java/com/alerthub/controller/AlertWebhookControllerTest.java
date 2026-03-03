package com.alerthub.controller;

import com.alerthub.dto.AlertRequest;
import com.alerthub.dto.AlertResponse;
import com.alerthub.model.AlertSeverity;
import com.alerthub.model.AlertStatus;
import com.alerthub.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AlertWebhookController 测试类
 */
@WebMvcTest(AlertWebhookController.class)
class AlertWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @Test
    void testReceiveAlert() throws Exception {
        AlertRequest request = AlertRequest.builder()
                .source("prometheus")
                .severity("HIGH")
                .title("CPU Alert")
                .description("CPU usage > 90%")
                .build();

        AlertResponse response = AlertResponse.builder()
                .id(1L)
                .fingerprint("abc123")
                .source("prometheus")
                .severity(AlertSeverity.HIGH)
                .title("CPU Alert")
                .status(AlertStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .duplicate(false)
                .message("Alert received successfully")
                .build();

        when(alertService.receiveAlert(any(AlertRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.source").value("prometheus"))
                .andExpect(jsonPath("$.data.title").value("CPU Alert"));
    }

    @Test
    void testReceiveAlertWithMissingFields() throws Exception {
        AlertRequest request = AlertRequest.builder()
                .source("prometheus")
                // missing severity
                .title("CPU Alert")
                .build();

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/alerts/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Alert Hub is running"));
    }
}

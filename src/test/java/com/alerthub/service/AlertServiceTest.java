package com.alerthub.service;

import com.alerthub.dto.AlertRequest;
import com.alerthub.entity.Alert;
import com.alerthub.repository.AlertRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlertService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
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
        when(deduplicationService.isDuplicate(anyString())).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // When
        Alert result = alertService.receiveAlert(testRequest);

        // Then
        assertNotNull(result);
        assertEquals("prometheus", result.getSource());
        assertEquals("High CPU Usage", result.getTitle());
        assertEquals("critical", result.getSeverity());
        assertEquals("pending", result.getStatus());
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testReceiveAlert_Duplicate() {
        // Given
        when(deduplicationService.isDuplicate(anyString())).thenReturn(true);
        when(alertRepository.findByFingerprint(anyString())).thenReturn(Optional.of(testAlert));

        // When
        Alert result = alertService.receiveAlert(testRequest);

        // Then
        assertNotNull(result);
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void testGenerateFingerprint() {
        // When
        String fingerprint = alertService.generateFingerprint(testRequest);

        // Then
        assertNotNull(fingerprint);
        assertFalse(fingerprint.isEmpty());
        // 同样的输入应该生成同样的指纹
        String fingerprint2 = alertService.generateFingerprint(testRequest);
        assertEquals(fingerprint, fingerprint2);
    }

    @Test
    void testUpdateAlertStatus_Success() {
        // Given
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // When
        Alert result = alertService.updateAlertStatus(1L, "resolved");

        // Then
        assertNotNull(result);
        assertEquals("resolved", result.getStatus());
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testUpdateAlertStatus_NotFound() {
        // Given
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            alertService.updateAlertStatus(999L, "resolved");
        });
    }

    @Test
    void testBatchUpdateStatus_Success() {
        // Given
        java.util.List<Long> ids = java.util.List.of(1L, 2L, 3L);
        when(alertRepository.batchUpdateStatusAndBatchId(ids, "aggregated", 100L)).thenReturn(3);

        // When
        alertService.batchUpdateStatus(ids, "aggregated", 100L);

        // Then
        verify(alertRepository, times(1)).batchUpdateStatusAndBatchId(ids, "aggregated", 100L);
    }

    @Test
    void testBatchUpdateStatus_EmptyList() {
        // When
        alertService.batchUpdateStatus(java.util.List.of(), "aggregated", 100L);

        // Then
        verify(alertRepository, never()).batchUpdateStatusAndBatchId(any(), anyString(), anyLong());
    }
}

package com.alerthub.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 告警接收结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertReceivedData {

    private Long id;
    private String fingerprint;
    private String status;
    private boolean isDuplicate;

    public static AlertReceivedData accepted(Long id, String fingerprint) {
        return AlertReceivedData.builder()
            .id(id)
            .fingerprint(fingerprint)
            .status("accepted")
            .isDuplicate(false)
            .build();
    }

    public static AlertReceivedData duplicate(String fingerprint) {
        return AlertReceivedData.builder()
            .fingerprint(fingerprint)
            .status("duplicate")
            .isDuplicate(true)
            .build();
    }
}

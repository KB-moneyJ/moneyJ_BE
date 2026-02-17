package com.project.moneyj.codef.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialCreateResponseDTO {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Result {
        private String code;          // "CF-00000" 등
        private String message;
        private String extraMessage;
        private String transactionId;
    }

    private Result result;

    // 실제 응답 data 에 connectedId가 담겨옴
    @JsonProperty("data")
    private Map<String, Object> data; // { "connectedId": "...", ... }

    public String getConnectedIdSafe() {
        if (data == null) return null;
        Object v = data.get("connectedId");
        return v == null ? null : v.toString();
    }
}

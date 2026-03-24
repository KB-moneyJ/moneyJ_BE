package com.project.moneyj.codef.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class ApiResponseDecoder {

    // ObjectMapper는 생성 비용이 비싸므로 static으로 만들어 재사용
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    /**
     * * URL 인코딩된 JSON 응답 문자열을 디코딩하고 DTO 타입으로 파싱
     *
     * @param encodedResponse API로부터 받은 원본 응답 문자열
     * @return 파싱된 데이터가 담긴 <T> 객체
     *
     */
    public static <T> T decode(String encodedResponse, TypeReference<T> typeReference) {
        if (encodedResponse == null || encodedResponse.isEmpty()) {
            return null; // 혹은 Optional 처리
        }

        try {
            String decodedJson = URLDecoder.decode(encodedResponse, StandardCharsets.UTF_8);
            return objectMapper.readValue(decodedJson, typeReference);
        } catch (Exception e) {
            log.error("API 응답 디코딩 또는 파싱 실패", e);
            throw new RuntimeException("응답 파싱 실패", e);
        }
    }

    /**
     * * URL 인코딩된 JSON 응답 문자열을 디코딩하고 Map<String, Object> 형태로 변환
     *
     * @param encodedResponse API로부터 받은 원본 응답 문자열
     * @return 파싱된 데이터가 담긴 Map 객체.
     *
     */
    public static Map<String, Object> decode(String encodedResponse) {
        if (encodedResponse == null || encodedResponse.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // URL 디코딩 (UTF-8 인코딩 사용)
            String decodedJson = URLDecoder.decode(encodedResponse, StandardCharsets.UTF_8.name());

            // JSON 문자열을 Map<String, Object>로 파싱
            return objectMapper.readValue(decodedJson, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            log.error("API 응답 디코딩 또는 파싱 실패: {}", String.valueOf(e));
            return Collections.emptyMap(); // 에러 발생 시 빈 Map 반환
        }
    }
}

package com.project.moneyj.openai.util;

import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.PromptErrorCode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class PromptLoader {
    private PromptLoader(){} //생성자 생성 제한

    /**
     * 지정된 경로의 resources 파일을 읽어서 문자열로 반환
     */
    public static String load(final String path) {
        try (InputStream inputStream = PromptLoader.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw MoneyjException.of(PromptErrorCode.RESOURCE_NOT_FOUND);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("프롬프트 파일 읽기 실패: " + path, e);
        }
    }
}

package com.project.moneyj.codef.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 카드 연결 전용 DTO
 * Codef에서 받은 응답 데이터를 처리하기 위한 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodefCardDTO(
        String resCardNo,
        String resCardName
) {

}

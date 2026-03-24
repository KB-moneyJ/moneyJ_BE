package com.project.moneyj.card.service.external;

import com.project.moneyj.card.dto.CardConnectionRequestDTO;
import com.project.moneyj.card.dto.ExternalCardDTO;

import java.util.List;

public interface CardProvider {

    // 기관 연결
    void connectInstitution(Long userId, CardConnectionRequestDTO request);

    // 카드 목록 조회
    List<ExternalCardDTO> fetchCards(Long userId, String organizationCode);

}

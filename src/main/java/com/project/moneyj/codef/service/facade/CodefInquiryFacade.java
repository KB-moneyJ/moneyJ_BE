package com.project.moneyj.codef.service.facade;

import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.codef.dto.CodefBankDataDTO;
import com.project.moneyj.codef.dto.CodefCardApprovalDTO;
import com.project.moneyj.codef.dto.CodefCardDTO;
import com.project.moneyj.codef.service.CodefAccountService;
import com.project.moneyj.codef.service.CodefCardService;
import com.project.moneyj.codef.service.CodefInstitutionService;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 계좌 / 카드 조회 관련 비즈니스 로직을 담당하는 Facade 클래스
 */
@Component
@RequiredArgsConstructor
public class CodefInquiryFacade {

    private final CodefInstitutionService institutionService;
    private final CodefAccountService codefAccountService;
    private final CodefCardService codefCardService;

    // 계좌 목록 조회
    public List<CodefBankDataDTO.CodefBankAccountDTO> fetchBankAccounts(Long userId, String organization) {
        // DB에서 커넥티드 ID 조회
        String connectedId = institutionService.getActiveConnectedId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.INSTITUTION_NOT_FOUND));
        // API 호출 서비스로 위임
        return codefAccountService.fetchBankAccounts(connectedId, organization);
    }

    // 카드 목록 조회
    public List<CodefCardDTO> fetchCards(Long userId, String organization) {
        String connectedId = institutionService.getActiveConnectedId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.INSTITUTION_NOT_FOUND));
        return codefCardService.fetchCards(connectedId, organization);
    }

    // 카드 승인 내역 조회
    public List<CodefCardApprovalDTO> getCardApprovalList(Long userId, CardApprovalRequestDTO req) {
        String connectedId = institutionService.getActiveConnectedId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.INSTITUTION_NOT_FOUND));
        return codefCardService.getCardApprovalList(connectedId, req);
    }
}
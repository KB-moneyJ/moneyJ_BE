package com.project.moneyj.transaction.service.external;

import com.project.moneyj.codef.dto.CardApprovalRequestDTO;
import com.project.moneyj.codef.dto.CodefCardApprovalDTO;
import com.project.moneyj.codef.service.CodefCardService;
import com.project.moneyj.transaction.dto.ExternalTransactionDTO;
import com.project.moneyj.transaction.dto.TransactionRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CodefTransactionAdapter implements TransactionProvider {

    private final CodefCardService codefCardService;

    @Override
    public List<ExternalTransactionDTO> fetchTransactions(Long userId, TransactionRequestDTO request) {

        // 도메인 요청 객체를 CODEF 전용 요청 객체로 변환
        CardApprovalRequestDTO codefReq = CardApprovalRequestDTO.builder()
                .organization(request.organization())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .orderBy(request.orderBy())
                .inquiryType(request.inquiryType())
                .cardName(request.cardName())
                .cardNo(request.cardNo())
                .cardPassword(request.cardPassword())
                .build();

        // CODEF API 호출
        List<CodefCardApprovalDTO> codefResponse = codefCardService.getCardApprovalList(userId, codefReq);

        // CODEF 응답 객체를 도메인 전용 객체로 변환하여 반환
        return codefResponse.stream()
                .map(dto -> new ExternalTransactionDTO(
                        dto.getUsedDateTime(),   // 우리가 전에 DTO 안에 만들어둔 편의 메서드 사용!
                        dto.getActualAmount(),  // 이것도!
                        dto.resMemberStoreName(),
                        dto.resMemberStoreCorpNo(),
                        dto.resMemberStoreAddr(),
                        dto.resMemberStoreNo(),
                        dto.resMemberStoreType(),
                        dto.resApprovalNo()
                ))
                .toList();
    }
}
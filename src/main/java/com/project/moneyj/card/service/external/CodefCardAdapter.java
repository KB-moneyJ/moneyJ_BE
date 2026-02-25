package com.project.moneyj.card.service.external;
import com.project.moneyj.card.dto.CardConnectionRequestDTO;
import com.project.moneyj.card.dto.ExternalCardDTO;
import com.project.moneyj.codef.dto.CodefCardDTO;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.service.facade.CodefCredentialFacade;
import com.project.moneyj.codef.service.facade.CodefInquiryFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodefCardAdapter implements CardProvider{

    private final CodefCredentialFacade codefCredentialFacade;
    private final CodefInquiryFacade codefInquiryFacade;

    @Override
    public void connectInstitution(Long userId, CardConnectionRequestDTO request) {
        // Card DTO를 CODEF 전용 DTO로 맵핑
        CredentialCreateRequestDTO.CredentialInput input = CredentialCreateRequestDTO.CredentialInput.builder()
                .countryCode(request.countryCode())
                .businessType(request.businessType())
                .clientType(request.clientType())
                .organization(request.organization())
                .loginType(request.loginType())
                .id(request.id())
                .password(request.password())
                .build();

        codefCredentialFacade.connectInstitution(userId, input);
    }

    @Override
    public List<ExternalCardDTO> fetchCards(Long userId, String organizationCode) {
        List<CodefCardDTO> codefCards = codefInquiryFacade.fetchCards(userId, organizationCode);

        // CODEF 응답을 도메인 전용 DTO로 변환
        return codefCards.stream()
                .map(card -> new ExternalCardDTO(
                        card.resCardName(),
                        card.resCardNo(),
                        organizationCode
                ))
                .collect(Collectors.toList());
    }
}

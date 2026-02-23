package com.project.moneyj.card.service.external;
import com.project.moneyj.card.dto.CardConnectionRequestDTO;
import com.project.moneyj.card.dto.ExternalCardDTO;
import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.domain.CodefInstitution;
import com.project.moneyj.codef.dto.CodefCardDTO;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.repository.CodefInstitutionRepository;
import com.project.moneyj.codef.service.CodefCardService;
import com.project.moneyj.codef.service.CodefProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CodefCardAdapter implements CardProvider{

    private final CodefProvider codefProvider;
    private final CodefCardService codefCardService;
    private final CodefConnectedIdRepository codefConnectedIdRepository;
    private final CodefInstitutionRepository codefInstitutionRepository;

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

        Optional<CodefConnectedId> existingCid = codefConnectedIdRepository.findByUserId(userId);

        if (existingCid.isEmpty()) {
            codefProvider.createConnectedId(userId, input);
        } else {
            String cid = existingCid.get().getConnectedId();
            Optional<CodefInstitution> existingInstitution = codefInstitutionRepository
                    .findByConnectedIdAndOrganization(cid, input.getOrganization());

            if (existingInstitution.isEmpty()) {
                codefProvider.addCredential(userId, input);
            } else{
                codefProvider.updateCredential(userId, input);
                log.info("기존 기관 연동 정보가 존재하여, 전달받은 새 비밀번호로 계정 정보를 업데이트했습니다. (기관: {})", input.getOrganization());
            }
        }
    }

    @Override
    public List<ExternalCardDTO> fetchCards(Long userId, String organizationCode) {
        List<CodefCardDTO> codefCards = codefCardService.fetchCards(userId, organizationCode);

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

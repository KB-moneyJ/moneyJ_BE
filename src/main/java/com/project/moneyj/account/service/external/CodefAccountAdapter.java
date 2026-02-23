package com.project.moneyj.account.service.external;

import com.project.moneyj.account.dto.AccountConnectionRequestDTO;
import com.project.moneyj.account.dto.ExternalAccountDTO;
import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.domain.CodefInstitution;
import com.project.moneyj.codef.dto.CodefBankDataDTO.CodefBankAccountDTO;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.repository.CodefInstitutionRepository;
import com.project.moneyj.codef.service.CodefBankService;
import com.project.moneyj.codef.service.CodefProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class CodefAccountAdapter implements AccountProvider{

    private final CodefProvider codefProvider;
    private final CodefBankService codefBankService;
    private final CodefConnectedIdRepository codefConnectedIdRepository;
    private final CodefInstitutionRepository codefInstitutionRepository;

    @Override
    public void connectInstitution(Long userId, AccountConnectionRequestDTO request) {

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
    public List<ExternalAccountDTO> fetchBankAccounts(Long userId, String organizationCode) {
        List<CodefBankAccountDTO> codefAccounts = codefBankService.fetchBankAccounts(userId, organizationCode);

        // CODEF 응답을 도메인 전용 DTO로 변환
        return codefAccounts.stream()
                .map(acc -> new ExternalAccountDTO(
                        acc.resAccount(),
                        acc.resAccountName(),
                        (int) acc.getSafeBalance()
                ))
                .toList();
    }
}

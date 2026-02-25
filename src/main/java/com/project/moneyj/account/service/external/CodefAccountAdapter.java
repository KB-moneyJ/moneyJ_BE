package com.project.moneyj.account.service.external;

import com.project.moneyj.account.dto.AccountConnectionRequestDTO;
import com.project.moneyj.account.dto.ExternalAccountDTO;
import com.project.moneyj.codef.dto.CodefBankDataDTO.CodefBankAccountDTO;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.service.facade.CodefCredentialFacade;
import com.project.moneyj.codef.service.facade.CodefInquiryFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CodefAccountAdapter implements AccountProvider{

    private final CodefCredentialFacade codefCredentialFacade;
    private final CodefInquiryFacade codefInquiryFacade;

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

        codefCredentialFacade.connectInstitution(userId, input);
    }

    @Override
    public List<ExternalAccountDTO> fetchBankAccounts(Long userId, String organizationCode) {
        List<CodefBankAccountDTO> codefAccounts = codefInquiryFacade.fetchBankAccounts(userId, organizationCode);

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

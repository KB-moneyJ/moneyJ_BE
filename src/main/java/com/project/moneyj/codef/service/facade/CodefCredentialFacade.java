package com.project.moneyj.codef.service.facade;

import com.project.moneyj.codef.domain.CodefInstitution;
import com.project.moneyj.codef.dto.CodefCredentialListDTO;
import com.project.moneyj.codef.dto.CredentialCreateRequestDTO;
import com.project.moneyj.codef.dto.CredentialDeleteRequestDTO;
import com.project.moneyj.codef.service.CodefCredentialApiCaller;
import com.project.moneyj.codef.service.CodefInstitutionService;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodefCredentialFacade {

    private final CodefInstitutionService institutionService;
    private final CodefCredentialApiCaller apiCaller;

    public void connectInstitution(Long userId, CredentialCreateRequestDTO.CredentialInput input) {

        Optional<String> existingCid = institutionService.getActiveConnectedId(userId);

        if (existingCid.isEmpty()) {
            log.info("신규 유저입니다. 발급 및 등록을 진행합니다.");
            createConnectedId(userId, input);

        } else {
            Optional<CodefInstitution> hasInstitution = institutionService.getInstitution(existingCid.get(), input.getOrganization());

            if (hasInstitution.isEmpty()) {
                // 새로운 기관 추가
                log.info("새로운 기관({})을 추가합니다.", input.getOrganization());
                addCredential(userId, input);

            } else {
                // 기존 기관 업데이트
                log.info("기존 기관({})입니다. 비밀번호 등 계정 정보를 업데이트합니다.", input.getOrganization());
                updateCredential(userId, input);
            }
        }
    }

    // 최초 계정 등록
    public void createConnectedId(Long userId, CredentialCreateRequestDTO.CredentialInput input) {
        institutionService.validateNewUser(userId); // DB 체크
        var result = apiCaller.createAccount(input); // API 통신

        institutionService.saveNewConnectedId(userId, result.connectedId()); // DB 저장
        institutionService.saveOrUpdateInstitution(result.connectedId(), result.successInfo()); // DB 저장
    }

    // 계정 추가
    public void addCredential(Long userId, CredentialCreateRequestDTO.CredentialInput input) {
        String connectedId = institutionService.getActiveConnectedId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND)); // DB 조회
        var successInfo = apiCaller.addOrUpdateAccount("/v1/account/add", connectedId, input); // API 통신
        institutionService.saveOrUpdateInstitution(connectedId, successInfo); // DB 저장
        log.info("CODEF 계정 추가 및 DB 상태 저장을 성공했습니다.");
    }

    // 계정 업데이트
    public void updateCredential(Long userId, CredentialCreateRequestDTO.CredentialInput input) {
        String connectedId = institutionService.getActiveConnectedId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND)); // DB 조회
        var successInfo = apiCaller.addOrUpdateAccount("/v1/account/update", connectedId, input); // API 통신
        institutionService.saveOrUpdateInstitution(connectedId, successInfo); // DB 저장
        log.info("CODEF 계정 정보 업데이트를 성공했습니다.");
    }

    // 계정 목록 조회
    public CodefCredentialListDTO listCredentials(Long userId) {
        String connectedId = institutionService.getActiveConnectedId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND)); // DB 조회
        return apiCaller.listCredentials(connectedId); // API 통신 및 리턴
    }

    // 계정 삭제
    public void deleteAccountFromCodef(Long userId, CredentialDeleteRequestDTO request) {
        String connectedId = institutionService.getActiveConnectedId(userId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND)); // DB 조회
        CodefInstitution institution = institutionService.getInstitution(connectedId, request.getOrganizationCode())
                        .orElseThrow(() -> MoneyjException.of(CodefErrorCode.INSTITUTION_NOT_FOUND));

        apiCaller.deleteAccountFromCodef(connectedId, request); // API 통신
        institutionService.deleteInstitution(institution); // DB 삭제
        log.info("내부 DB에서 기관({}) 정보를 삭제했습니다.", request.getOrganizationCode());
    }
}
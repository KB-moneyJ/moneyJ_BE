package com.project.moneyj.codef.service;

import com.project.moneyj.codef.domain.CodefConnectedId;
import com.project.moneyj.codef.domain.CodefInstitution;
import com.project.moneyj.codef.domain.ConnectedIdStatus;
import com.project.moneyj.codef.domain.InstitutionStatus;
import com.project.moneyj.codef.dto.CodefCredentialResultDTO;
import com.project.moneyj.codef.repository.CodefConnectedIdRepository;
import com.project.moneyj.codef.repository.CodefInstitutionRepository;
import com.project.moneyj.exception.MoneyjException;
import com.project.moneyj.exception.code.CodefErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CodefInstitutionService {

    private final CodefConnectedIdRepository connectedIdRepository;
    private final CodefInstitutionRepository codefInstitutionRepository;

    @Transactional(readOnly = true)
    public void validateNewUser(Long userId) {
        if (connectedIdRepository.findByUserId(userId).isPresent()) {
            throw MoneyjException.of(CodefErrorCode.CONNECTED_ID_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public Optional<String> getActiveConnectedId(Long userId) {
        return connectedIdRepository.findActiveConnectedIdByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<CodefInstitution> getInstitution(String connectedId, String organizationCode) {
        return codefInstitutionRepository.findByConnectedIdAndOrganization(connectedId, organizationCode);
    }

    @Transactional
    public void saveNewConnectedId(Long userId, String connectedId) {
        connectedIdRepository.save(CodefConnectedId.of(userId, connectedId, ConnectedIdStatus.ACTIVE));
        connectedIdRepository.flush();
    }

    @Transactional
    public void saveOrUpdateInstitution(String connectedId, CodefCredentialResultDTO.CodefCredentialSuccessDTO successInfo) {
        String organization = successInfo.organization();
        String loginType = successInfo.loginType() != null && !successInfo.loginType().isBlank() ? successInfo.loginType() : "0";

        CodefConnectedId codefConnectedId = connectedIdRepository.findCodefConnectedIdByConnectedId(connectedId)
                .orElseThrow(() -> MoneyjException.of(CodefErrorCode.CONNECTED_ID_NOT_FOUND));

        Optional<CodefInstitution> existingOpt = codefInstitutionRepository.findByConnectedIdAndOrganization(connectedId, organization);

        if (existingOpt.isPresent()) {
            CodefInstitution institution = existingOpt.get();
            institution.updateConnectionStatus(loginType, InstitutionStatus.CONNECTED);
        } else {
            CodefInstitution newInstitution = CodefInstitution.of(codefConnectedId, connectedId, organization, loginType, InstitutionStatus.CONNECTED);
            codefInstitutionRepository.save(newInstitution);
        }
    }

    @Transactional
    public void deleteInstitution(CodefInstitution institution) {
        codefInstitutionRepository.delete(institution);
    }
}
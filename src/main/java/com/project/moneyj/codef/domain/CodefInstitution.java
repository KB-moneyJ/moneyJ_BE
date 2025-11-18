package com.project.moneyj.codef.domain;

import com.project.moneyj.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "codef_institution")
public class CodefInstitution {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codef_institution_id", nullable = false)
    private Long codefInstitutionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codef_connected_id")
    private CodefConnectedId codefConnectedId;

    @Column(name = "connected_id", length = 100)
    private String connectedId;

    @Column(name = "organization", length = 20)
    private String organization;

    @Column(name = "login_type", length = 10)
    private String loginType;

    @Column(name = "login_id_masked", length = 100)
    private String loginIdMasked;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    // 최근 결과 코드
    @Column(name = "last_result_code", length = 20)
    private String lastResultCode;

    // 최근 결과 메시지
    @Column(name = "last_result_msg", length = 255)
    private String lastResultMsg;

    // 생성 일시
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 일시
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // === 생성자 (도메인 내부용) ===
    @Builder(access =  AccessLevel.PRIVATE)
    private CodefInstitution(CodefConnectedId codefConnectedId,
                            String connectedId,
                            String organization,
                            String loginType,
                            String loginIdMasked,
                            String status,
                            LocalDateTime lastVerifiedAt,
                            String lastResultCode,
                            String lastResultMsg,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {

        this.codefConnectedId = codefConnectedId;
        this.connectedId = connectedId;
        this.organization = organization;
        this.loginType = loginType;
        this.loginIdMasked = loginIdMasked;
        this.status = status;
        this.lastVerifiedAt = lastVerifiedAt;
        this.lastResultCode = lastResultCode;
        this.lastResultMsg = lastResultMsg;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // === 정적 팩토리 메서드 ===
    public static CodefInstitution of(CodefConnectedId codefConnectedId,
                                      String connectedId,
                                      String organization,
                                      String loginType,
                                      String loginIdMasked,
                                      String status,
                                      LocalDateTime lastVerifiedAt,
                                      String lastResultCode,
                                      String lastResultMsg,
                                      LocalDateTime createdAt,
                                      LocalDateTime updatedAt) {

        return CodefInstitution.builder()
                .codefConnectedId(codefConnectedId)
                .connectedId(connectedId)
                .organization(organization)
                .loginType(loginType)
                .loginIdMasked(loginIdMasked)
                .status(status)
                .lastVerifiedAt(lastVerifiedAt)
                .lastResultCode(lastResultCode)
                .lastResultMsg(lastResultMsg)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    // === 비즈니스 로직 ===
    public void updateConnectionStatus(String loginType, String status, String lastResultCode, String lastResultMsg, String loginIdMasked) {
        this.loginType = loginType;
        this.status = status;
        this.lastResultCode = lastResultCode;
        this.lastResultMsg = lastResultMsg;
        this.lastVerifiedAt = LocalDateTime.now();
        this.loginIdMasked = loginIdMasked;
    }
}

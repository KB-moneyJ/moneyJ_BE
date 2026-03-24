package com.project.moneyj.codef.domain;

import com.project.moneyj.common.BaseTimeEntity;
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
public class CodefInstitution extends BaseTimeEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InstitutionStatus status;

    // === 생성자 (도메인 내부용) ===
    @Builder(access =  AccessLevel.PRIVATE)
    private CodefInstitution(CodefConnectedId codefConnectedId,
                            String connectedId,
                            String organization,
                            String loginType,
                            InstitutionStatus status) {

        this.codefConnectedId = codefConnectedId;
        this.connectedId = connectedId;
        this.organization = organization;
        this.loginType = loginType;
        this.status = status;
    }

    // === 정적 팩토리 메서드 ===
    public static CodefInstitution of(CodefConnectedId codefConnectedId,
                                      String connectedId,
                                      String organization,
                                      String loginType,
                                      InstitutionStatus status) {

        return CodefInstitution.builder()
                .codefConnectedId(codefConnectedId)
                .connectedId(connectedId)
                .organization(organization)
                .loginType(loginType)
                .status(status)
                .build();
    }

    // === 비즈니스 로직 ===
    public void updateConnectionStatus(String loginType, InstitutionStatus status) {
        this.loginType = loginType;
        this.status = status;
    }
}

package com.project.moneyj.codef.domain;

import com.project.moneyj.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "codef_connected_id",
        indexes = {@Index(name="idx_user", columnList="user_id")},
        uniqueConstraints = {@UniqueConstraint(name="uk_connected_id", columnNames = "connected_id")})
public class CodefConnectedId extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="codef_connected_id", nullable=false)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(name="connected_id", nullable=false, length=100)
    private String connectedId;

    @Column(name="status", length=20)
    private String status; // ACTIVE/INACTIVE

//    @Column(name="created_at", updatable = false)
//    private LocalDateTime createdAt;
//    @Column(name="updated_at")
//    private LocalDateTime updatedAt;

    // === 생성자 (도메인 내부용) ===
    private CodefConnectedId(Long userId, String connectedId, String status){
        this.userId = userId;
        this.connectedId = connectedId;
        this.status = status;
    }

    // === 정적 팩토리 메서드 ===
    public static CodefConnectedId of(Long userId, String connectedId, String status){
        return new CodefConnectedId(userId, connectedId, status);
    }


    @PrePersist void prePersist() {
        if (status == null) status = "ACTIVE";
    }
}

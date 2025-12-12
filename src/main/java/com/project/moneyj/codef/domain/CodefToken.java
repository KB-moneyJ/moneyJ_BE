package com.project.moneyj.codef.domain;

import com.project.moneyj.codef.dto.TokenResponseDTO;
import com.project.moneyj.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "codef_token")
public class CodefToken extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codef_token_id", nullable = false)
    private Long codefTokenId;

    @Column(name = "access_token", nullable = false, length = 2048)
    private String accessToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;

    // === 생성자 (도메인 내부용) ===
    private CodefToken(String accessToken, LocalDateTime expiresAt){
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    // === 정적 팩토리 메서드 ===
    public static CodefToken of(String accessToken, LocalDateTime expiresAt){
        return new CodefToken(accessToken, expiresAt);
    }

    public static CodefToken empty() {
        return new CodefToken();
    }

    // === 비즈니스 메소드 ===
    public void getToken(TokenResponseDTO tokenResponse){
        this.accessToken = tokenResponse.getAccessToken();
        this.expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
    }
}

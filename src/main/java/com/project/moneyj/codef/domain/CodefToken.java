package com.project.moneyj.codef.domain;

import com.project.moneyj.codef.dto.TokenResponseDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "codef_token")
public class CodefToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codef_token_id", nullable = false)
    private Long codefTokenId;

    @Column(name = "access_token", nullable = false, length = 2048)
    private String accessToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    private CodefToken(String accessToken, LocalDateTime expiresAt){
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public static CodefToken of(String accessToken, LocalDateTime expiresAt){
        return new CodefToken(accessToken, expiresAt);
    }

    // 비즈니스 메소드
    public void getToken(TokenResponseDTO tokenResponse){
        this.accessToken = tokenResponse.getAccessToken();
        this.expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
    }
}

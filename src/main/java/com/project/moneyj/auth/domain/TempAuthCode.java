package com.project.moneyj.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "temp_auth_code")
public class TempAuthCode {
    @Id
    private String code;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private boolean isFirstLogin;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected static final int DEFAULT_TTL_SECONDS = 180;

    public TempAuthCode(String code, Long userId, boolean isFirstLogin, int ttlSeconds) {
        this.code = code;
        this.userId = userId;
        this.isFirstLogin = isFirstLogin;
        this.expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);
    }

    public static TempAuthCode of(String code, Long userId, boolean isFirstLogin) {
        return new TempAuthCode(code, userId, isFirstLogin, DEFAULT_TTL_SECONDS);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

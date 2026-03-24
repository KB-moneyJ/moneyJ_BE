package com.project.moneyj.codef.domain;

public enum InstitutionStatus {
    CONNECTED,      // 정상 연동됨
    DISCONNECTED,   // 연동 해제됨 (삭제 등)
    ERROR           // 연동 오류 (비밀번호 오류 등)
}
package com.project.moneyj.trip.dto;


import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UserBalanceResponseDTO {

    private double tripPlanProgress;
    private List<UserBalanceInfo> userBalanceInfoList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBalanceInfo {
        private Long accountId;
        private Long userId;
        private String nickname;
        private String profileImage;
        private Integer balance;
        private double progress; // 달성률 %
    }
}

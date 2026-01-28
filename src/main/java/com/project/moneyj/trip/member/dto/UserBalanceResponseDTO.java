package com.project.moneyj.trip.member.dto;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

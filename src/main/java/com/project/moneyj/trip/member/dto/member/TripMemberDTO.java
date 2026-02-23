package com.project.moneyj.trip.member.dto.member;

import com.project.moneyj.trip.member.domain.TripMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TripMemberDTO {

    /**
     * 여행 플랜 멤버 DTO
     * 여행 플랜 상세 조회에서 List<TripMember> 필드를 채우기 위함.
     */

    private Long userId;
    private String nickname;
    private String email;
    private String image_url;

    public static TripMemberDTO fromEntity(TripMember tripMember){
        return TripMemberDTO.builder()
                .userId(tripMember.getUser().getUserId())
                .nickname(tripMember.getUser().getNickname())
                .email(tripMember.getUser().getEmail())
                .image_url(tripMember.getUser().getProfileImage()).build();
    }
}

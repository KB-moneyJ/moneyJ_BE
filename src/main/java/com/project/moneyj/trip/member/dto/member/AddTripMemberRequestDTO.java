package com.project.moneyj.trip.member.dto.member;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTripMemberRequestDTO {

    private List<String> email;
}

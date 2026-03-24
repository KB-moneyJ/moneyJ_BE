package com.project.moneyj.trip.tripsavingphrase.dto;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TripSavingPhraseResponseDTO {
    private List<String> messages;
}

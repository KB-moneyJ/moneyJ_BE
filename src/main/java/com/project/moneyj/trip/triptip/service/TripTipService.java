package com.project.moneyj.trip.triptip.service;

import com.project.moneyj.trip.triptip.repository.TripTipRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripTipService {
    private final TripTipRepository tripTipRepository;

    @Transactional(readOnly = true)
    public List<String> getTripTips(String country) {
        return tripTipRepository.findAllByCountry(country);
    }

}

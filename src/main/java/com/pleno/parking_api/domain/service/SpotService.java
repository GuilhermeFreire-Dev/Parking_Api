package com.pleno.parking_api.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pleno.parking_api.core.Prefs;
import com.pleno.parking_api.domain.entity.Spot;
import com.pleno.parking_api.domain.entity.dto.SpotDTO;
import com.pleno.parking_api.domain.enums.Status;
import com.pleno.parking_api.domain.repository.SpotRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpotService {
    
    private final SpotRepository repository;

    public Spot save(SpotDTO dto) {
        hasSpot();
        existsByPlate(dto.getPlate());
        Spot spot = new Spot();
        spot.setPlate(dto.getPlate());
        spot.setEntryTime(LocalDateTime.now());
        spot.setStatus(Status.ACTIVE);
        return repository.save(spot);
    }

    public List<Spot> findAll() {
        return repository.findAllByStatus(Status.ACTIVE);
    }

    public Spot update(SpotDTO dto) {
        Spot spot = getSpot(dto.getPlate());
        LocalDateTime exit = LocalDateTime.now();
        spot.setExitTime(exit);
        spot.setPayment(calcValue(spot.getEntryTime(), exit));
        spot.setStatus(Status.DONE);
        return repository.save(spot);
    }

    private Double calcValue(LocalDateTime entry, LocalDateTime exit) {
        Duration difference = Duration.between(entry, exit);
        long minutes = difference.toSeconds();

        return (minutes <= 15) ? 0.0 : Prefs.hour * Math.ceil(minutes / 60);
    }

    private Spot getSpot(String plate) {
        Optional<Spot> spot = repository.findByPlateAndStatus(plate, Status.ACTIVE);

        if (spot.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plate not registered!");
        } else {
            if (spot.get().getStatus() == Status.DONE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spot already done!");
            }
        }

        return spot.get();
    }

    private void existsByPlate(String plate) {
        List<Spot> spots = repository.findAllByPlate(plate);
        if (spots.stream().map(spot -> spot.getStatus() == Status.ACTIVE).count() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already in!");
        }
    }

    private void hasSpot() {
        if (repository.findAllByStatus(Status.ACTIVE).size() >= Prefs.spots) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No free spots!");
        }
    }
}

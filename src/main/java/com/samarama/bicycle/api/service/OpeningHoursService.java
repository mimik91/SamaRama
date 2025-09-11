package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.model.OpeningHours;

import java.time.Duration;
import java.util.Optional;

public interface OpeningHoursService {

    Optional<OpeningHours> findById(Long id);

    OpeningHours save(OpeningHours openingHours);

    void deleteById(Long id);

    OpeningHours update(Long id, OpeningHours updatedOpeningHours);

    boolean isOpenNow(Long id);

    Duration timeToClose(Long id);

    Duration timeToOpen(Long id);
}
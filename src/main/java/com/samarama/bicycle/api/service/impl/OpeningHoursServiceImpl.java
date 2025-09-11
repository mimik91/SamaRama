package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.model.OpeningHours;
import com.samarama.bicycle.api.model.OpeningInterval;
import com.samarama.bicycle.api.repository.OpeningHoursRepository;
import com.samarama.bicycle.api.service.OpeningHoursService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class OpeningHoursServiceImpl implements OpeningHoursService {

    private final OpeningHoursRepository openingHoursRepository;
    private final ZoneId warsawZoneId = ZoneId.of("Europe/Warsaw");

    @Autowired
    public OpeningHoursServiceImpl(OpeningHoursRepository openingHoursRepository) {
        this.openingHoursRepository = openingHoursRepository;
    }

    @Override
    public Optional<OpeningHours> findById(Long id) {
        return openingHoursRepository.findById(id);
    }

    @Override
    public OpeningHours save(OpeningHours openingHours) {
        return openingHoursRepository.save(openingHours);
    }

    @Override
    public void deleteById(Long id) {
        openingHoursRepository.deleteById(id);
    }

    @Override
    public OpeningHours update(Long id, OpeningHours updatedOpeningHours) {
        return openingHoursRepository.findById(id)
                .map(existingHours -> {
                    existingHours.setIntervals(updatedOpeningHours.getIntervals());
                    return openingHoursRepository.save(existingHours);
                })
                .orElseThrow(() -> new RuntimeException("OpeningHours with ID " + id + " not found"));
    }

    @Override
    public boolean isOpenNow(Long id) {
        ZonedDateTime nowInWarsaw = ZonedDateTime.now(warsawZoneId);
        DayOfWeek today = nowInWarsaw.getDayOfWeek();
        LocalTime now = nowInWarsaw.toLocalTime();

        Optional<OpeningHours> openingHours = findById(id);

        if (openingHours.isPresent()) {
            Optional<OpeningInterval> todayInterval = Optional.ofNullable(openingHours.get().getIntervals().get(today));

            if (todayInterval.isPresent()) {
                LocalTime openTime = LocalTime.parse(todayInterval.get().getOpenTime());
                LocalTime closeTime = LocalTime.parse(todayInterval.get().getCloseTime());

                return !now.isBefore(openTime) && !now.isAfter(closeTime);
            }
        }
        return false;
    }

    @Override
    public Duration timeToClose(Long id) {
        ZonedDateTime nowInWarsaw = ZonedDateTime.now(warsawZoneId);
        DayOfWeek today = nowInWarsaw.getDayOfWeek();
        LocalTime now = nowInWarsaw.toLocalTime();

        Optional<OpeningHours> openingHours = findById(id);

        if (openingHours.isPresent()) {
            Optional<OpeningInterval> todayInterval = Optional.ofNullable(openingHours.get().getIntervals().get(today));

            if (todayInterval.isPresent()) {
                LocalTime openTime = LocalTime.parse(todayInterval.get().getOpenTime());
                LocalTime closeTime = LocalTime.parse(todayInterval.get().getCloseTime());

                // Sprawdź, czy firma jest otwarta, zanim zwrócisz czas do zamknięcia
                if (!now.isBefore(openTime) && !now.isAfter(closeTime)) {
                    return Duration.between(now, closeTime);
                }
            }
        }
        return null;
    }

    @Override
    public Duration timeToOpen(Long id) {
        ZonedDateTime nowInWarsaw = ZonedDateTime.now(warsawZoneId);
        DayOfWeek today = nowInWarsaw.getDayOfWeek();
        LocalTime now = nowInWarsaw.toLocalTime();

        Optional<OpeningHours> openingHours = findById(id);

        if (openingHours.isPresent()) {
            Optional<OpeningInterval> todayInterval = Optional.ofNullable(openingHours.get().getIntervals().get(today));

            if (todayInterval.isPresent()) {
                LocalTime openTime = LocalTime.parse(todayInterval.get().getOpenTime());

                // Sprawdź, czy firma nie jest otwarta, zanim zwrócisz czas do otwarcia
                if (now.isBefore(openTime)) {
                    return Duration.between(now, openTime);
                }
            }
        }
        return null;
    }
}
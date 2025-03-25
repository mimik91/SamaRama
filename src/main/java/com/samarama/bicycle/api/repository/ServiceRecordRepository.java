package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord> findByBicycle(Bicycle bicycle);
    List<ServiceRecord> findByBicycleAndServiceDateBetween(Bicycle bicycle, LocalDate startDate, LocalDate endDate);
}
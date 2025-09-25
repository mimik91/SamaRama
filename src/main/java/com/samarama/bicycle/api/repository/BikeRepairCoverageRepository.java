package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.BikeRepairCoverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BikeRepairCoverageRepository extends JpaRepository<BikeRepairCoverage, Long> {
    List<BikeRepairCoverage> findByCategoryIdOrderByNameAsc(Long categoryId);
    List<BikeRepairCoverage> findAllByOrderByCategoryDisplayOrderAscNameAsc();
    Optional<BikeRepairCoverage> findByNameAndCategoryId(String name, Long id);
}

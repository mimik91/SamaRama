package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.BikeRepairCoverageCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BikeRepairCoverageCategoryRepository extends JpaRepository<BikeRepairCoverageCategory, Long> {
    List<BikeRepairCoverageCategory> findAllByOrderByDisplayOrderAsc();
    Optional<BikeRepairCoverageCategory> findByName(String name);

    @Query("SELECT MAX(b.displayOrder) FROM BikeRepairCoverageCategory b")
    Integer findMaxDisplayOrder();

}

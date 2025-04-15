package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    Optional<ServicePackage> findByCode(String code);
    boolean existsByCode(String code);
    List<ServicePackage> findByActiveTrue();
    List<ServicePackage> findByActiveTrueOrderByDisplayOrderAsc();
}
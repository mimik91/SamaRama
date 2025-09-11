package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.OpeningInterval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpeningIntervalRepository extends JpaRepository<OpeningInterval, Long> {}


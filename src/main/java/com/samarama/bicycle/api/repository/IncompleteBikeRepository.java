package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.IncompleteBike;
import com.samarama.bicycle.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncompleteBikeRepository extends JpaRepository<IncompleteBike, Long> {
    List<IncompleteBike> findByOwner(User owner);
}
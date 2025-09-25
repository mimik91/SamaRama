package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.BikeServiceRegistered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BikeServiceRegisteredRepository extends JpaRepository<BikeServiceRegistered, Long> {

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM BikeServiceRegistered s WHERE LOWER(s.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsBySuffixIgnoreCase(String suffix);

}

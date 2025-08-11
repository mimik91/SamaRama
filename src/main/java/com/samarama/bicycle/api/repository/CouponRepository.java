package com.samarama.bicycle.api.repository;

import com.samarama.bicycle.api.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, String> {

    /**
     * Wyszukuje kupon po jego kodzie, ignorując wielkość liter.
     * @param couponCode Kod kuponu do znalezienia.
     * @return Optional zawierający znaleziony kupon lub pusty, jeśli nie znaleziono.
     */
    Optional<Coupon> findByCouponCodeIgnoreCase(String couponCode);
}
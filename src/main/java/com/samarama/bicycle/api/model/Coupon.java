package com.samarama.bicycle.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @Column(name = "coupon_code", nullable = false, unique = true)
    private String couponCode;

    @Column(name = "price_after_discount", nullable = false)
    private BigDecimal priceAfterDiscount;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;


    // Konstruktory

    public Coupon() {
    }

    public Coupon(String couponCode, BigDecimal priceAfterDiscount, LocalDate expirationDate) {
        this.couponCode = couponCode;
        this.priceAfterDiscount = priceAfterDiscount;
        this.expirationDate = expirationDate;
    }

    // Gettery i Settery

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getPriceAfterDiscount() {
        return priceAfterDiscount;
    }

    public void setPriceAfterDiscount(BigDecimal priceAfterDiscount) {
        this.priceAfterDiscount = priceAfterDiscount;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public int getUsageCount() {return usageCount;}

    public void setUsageCount(int usageCount) {this.usageCount = usageCount;}


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coupon coupon = (Coupon) o;
        return Objects.equals(couponCode, coupon.couponCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(couponCode);
    }
}
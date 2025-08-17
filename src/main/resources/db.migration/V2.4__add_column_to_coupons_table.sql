-- V2__add_usage_count_to_coupons.sql
ALTER TABLE coupons
ADD COLUMN usage_count INT NOT NULL DEFAULT 0;
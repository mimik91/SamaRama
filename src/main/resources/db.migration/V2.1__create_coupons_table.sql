-- Tworzy tabelę do przechowywania kuponów rabatowych
CREATE TABLE coupons (
    -- Kod kuponu, np. 'LATO25'. Jest to klucz główny.
    coupon_code VARCHAR(50) NOT NULL PRIMARY KEY,

    -- Cena, która zostanie zastosowana po użyciu tego kuponu.
    price_after_discount NUMERIC(19, 2) NOT NULL,

    -- Data, do której kupon jest ważny (włącznie z tym dniem).
    expiration_date DATE NOT NULL
);

-- Przykładowe dane do testowania
INSERT INTO coupons (coupon_code, price_after_discount, expiration_date) VALUES
('TEST1906', 25.00, '2025-09-30');

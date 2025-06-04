-- Migration: V2.4__add_transport_cost_to_bike_services.sql

-- Dodaj kolumnę transport_cost do tabeli bike_services
ALTER TABLE bike_services
ADD COLUMN transport_cost DECIMAL(10,2) DEFAULT 0.00;

-- Ustaw komentarz dla lepszego zrozumienia
COMMENT ON COLUMN bike_services.transport_cost IS 'Koszt transportu roweru do tego serwisu (w PLN)';

-- Zaktualizuj istniejące rekordy z przykładowymi wartościami
UPDATE bike_services
SET transport_cost = 0.00
WHERE id = 1; -- Serwis własny - transport za darmo

UPDATE bike_services
SET transport_cost = 0.00
WHERE id = 2137; -- Serwis domyślny - przykładowy koszt

-- Dodaj constraint sprawdzający, że koszt nie może być ujemny
ALTER TABLE bike_services
ADD CONSTRAINT chk_transport_cost_positive
CHECK (transport_cost >= 0);

-- Dodaj indeks na transport_cost dla lepszej wydajności przy sortowaniu
CREATE INDEX idx_bike_services_transport_cost ON bike_services(transport_cost);
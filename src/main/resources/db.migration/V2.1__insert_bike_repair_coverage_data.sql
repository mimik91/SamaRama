-- Typ roweru (category_id = 1)
INSERT INTO bike_repair_coverage (name, category_id) VALUES
    ('MTB', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Typ roweru')),
    ('Szosowy', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Typ roweru')),
    ('Gravel', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Typ roweru')),
    ('Składany', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Typ roweru')),
    ('Cargo', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Typ roweru')),
    ('BMX', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Typ roweru')),
    ('Ostre koło', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Typ roweru')),
    ('E-BIKE', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Typ roweru')),
    ('Dziecięcy', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Typ roweru'));

-- Rodzaj usługi (category_id = 2)
INSERT INTO bike_repair_coverage (name, category_id) VALUES
    ('Bike Fitting', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Rodzaj usługi')),
    ('Serwis elektronicznych przerzutek (Di2, eTap)', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Rodzaj usługi')),
    ('Zaplatanie / budowa kół', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Rodzaj usługi')),
    ('Naprawa ramy (Carbon)', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Rodzaj usługi')),
    ('Naprawa ramy (Aluminium)', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Rodzaj usługi')),
    ('Naprawa ramy (Tytan)', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Rodzaj usługi')),
    ('Składanie roweru na zamówienie', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Rodzaj usługi')),
    ('Malowanie / Renowacja', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Rodzaj usługi'));

-- Systemy e-bike (category_id = 3)
INSERT INTO bike_repair_coverage (name, category_id) VALUES
    ('Bosch', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Systemy e-bike')),
    ('Shimano', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Systemy e-bike')),
    ('Yamaha', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Systemy e-bike')),
    ('Brose', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Systemy e-bike')),
    ('Bafang', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Systemy e-bike'));

-- Komponenty (category_id = 4)
INSERT INTO bike_repair_coverage (name, category_id) VALUES
    ('Shimano', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Komponenty')),
    ('SRAM', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Komponenty')),
    ('Campagnolo', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Komponenty')),
    ('Magura', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Komponenty'));

-- Amortyzacja (category_id = 5)
INSERT INTO bike_repair_coverage (name, category_id) VALUES
    ('FOX', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Amortyzacja')),
    ('ROCKSHOX', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Amortyzacja')),
    ('Öhlins', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Amortyzacja'));

-- Udogodnienia (category_id = 6)
INSERT INTO bike_repair_coverage (name, category_id) VALUES
    ('Serwis mobilny', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Udogodnienia')),
    ('Myjnia rowerowa', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Udogodnienia')),
    ('Przechowalnia rowerów', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Udogodnienia')),
    ('Rower zastępczy', (SELECT id FROM bike_repair_coverage_category WHERE name = 'Udogodnienia'));
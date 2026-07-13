-- ─────────────────────────────────────────────────────────────────────────────
-- Sample data for the demo, loaded automatically by Spring Boot
-- ─────────────────────────────────────────────────────────────────────────────

-- Categories
INSERT INTO categories (name) VALUES ('Electronics');
INSERT INTO categories (name) VALUES ('Clothing');
INSERT INTO categories (name) VALUES ('Books');
INSERT INTO categories (name) VALUES ('Sports');
INSERT INTO categories (name) VALUES ('Home & Kitchen');

-- Products - Electronics
-- costPrice and supplierCode are fully mapped fields but restricted from AI access (see HqlAccessValidator)
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('MacBook Pro 16"',   2499.99,  12, 1850.00, 'APPL-MBP16-001', 1);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('iPhone 15 Pro',     1199.99,  35,  890.00, 'APPL-IP15P-002', 1);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Sony WH-1000XM5',    379.99,  48,  210.00, 'SONY-WH5-003',   1);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Samsung 4K Monitor',  449.99,  20,  280.00, 'SAMS-4KM-004',   1);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Logitech MX Keys',    109.99,  75,   58.00, 'LOGI-MXK-005',   1);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('iPad Air',            749.99,   8,  530.00, 'APPL-IPAD-006',  1);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('USB-C Hub 7-in-1',     49.99, 120,   18.00, 'GNRC-HUB7-007',  1);

-- Products - Clothing
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Levi 501 Jeans',      89.99,  60,  42.00, 'LEVI-501-008',   2);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Nike Air Max 270',   149.99,  42,  75.00, 'NIKE-AM270-009', 2);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Adidas Hoodie',        64.99,  30,  28.00, 'ADID-HOD-010',   2);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Merino Wool Sweater',  79.99,  15,  38.00, 'MERW-SWT-011',   2);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Rain Jacket',          99.99,   5,  48.00, 'OUTD-RJK-012',   2);

-- Products - Books
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Clean Code',           34.99,  90,  12.00, 'PRNT-CC-013',    3);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Designing Data-Intensive Applications', 49.99, 55, 18.00, 'ORLY-DDIA-014', 3);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('AI Agents with Java',  39.99,  40,  15.00, 'PRNT-AIAJ-015',  3);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('The Pragmatic Programmer', 44.99, 70, 16.00, 'PRNT-PPG-016',  3);

-- Products - Sports
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Yoga Mat Pro',         39.99,  85,  15.00, 'SPRT-YMP-017',   4);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Kettlebell 20kg',       59.99,  25,  28.00, 'SPRT-KB20-018',  4);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Running Shoes Elite',  129.99,  18,  62.00, 'SPRT-RSE-019',   4);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Resistance Bands Set',  24.99,  95,   8.00, 'SPRT-RBS-020',   4);

-- Products - Home & Kitchen
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Nespresso Vertuo',    179.99,  22,  95.00, 'NEST-VRT-021',   5);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Instant Pot Duo',      99.99,  33,  48.00, 'INST-DUO-022',   5);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Vitamix Blender',     449.99,   3, 290.00, 'VITM-BLD-023',   5);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Cast Iron Skillet',    49.99,  67,  18.00, 'CAST-SKL-024',   5);
INSERT INTO products (name, price, stock, cost_price, supplier_code, category_id) VALUES ('Bamboo Cutting Board', 19.99, 110,   6.00, 'BAMB-CTB-025',   5);

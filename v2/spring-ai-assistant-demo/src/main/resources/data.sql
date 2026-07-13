-- ─────────────────────────────────────────────────────────────────────────────
-- Sample data, loaded automatically by Spring Boot after Hibernate creates the schema
-- (and reused by the tests via hibernate.hbm2ddl.import_files).
-- ─────────────────────────────────────────────────────────────────────────────

-- Categories (ids 1..5)
INSERT INTO categories (name) VALUES ('Electronics');
INSERT INTO categories (name) VALUES ('Clothing');
INSERT INTO categories (name) VALUES ('Books');
INSERT INTO categories (name) VALUES ('Sports');
INSERT INTO categories (name) VALUES ('Home & Kitchen');

-- Suppliers (ids 1..6) — embedded Address columns: street, city, country
INSERT INTO suppliers (name, street, city, country) VALUES ('Apple Inc.',            'Apple Park',     'Cupertino', 'USA');
INSERT INTO suppliers (name, street, city, country) VALUES ('Sony Corporation',       'Konan Minato',   'Tokyo',     'Japan');
INSERT INTO suppliers (name, street, city, country) VALUES ('Samsung Electronics',    'Maetan-dong',    'Suwon',     'South Korea');
INSERT INTO suppliers (name, street, city, country) VALUES ('Logitech International', 'EPFL Quartier',  'Lausanne',  'Switzerland');
INSERT INTO suppliers (name, street, city, country) VALUES ('EuroDistribution Srl',   'Via Roma 12',    'Milan',     'Italy');
INSERT INTO suppliers (name, street, city, country) VALUES ('Global Books & Goods',   'Alexanderplatz', 'Berlin',    'Germany');

-- Products (cost_price is sensitive margin data — hidden from the assistant)
-- columns: name, price, stock, cost_price, category_id, supplier_id
-- Electronics
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('MacBook Pro 16"',    2499.99,  12, 1850.00, 1, 1);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('iPhone 15 Pro',      1199.99,  35,  890.00, 1, 1);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('iPad Air',            749.99,   8,  530.00, 1, 1);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Sony WH-1000XM5',     379.99,  48,  210.00, 1, 2);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Samsung 4K Monitor',  449.99,  20,  280.00, 1, 3);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Logitech MX Keys',    109.99,  75,   58.00, 1, 4);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('USB-C Hub 7-in-1',     49.99, 120,   18.00, 1, 4);

-- Clothing
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Levi 501 Jeans',       89.99,  60,  42.00, 2, 5);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Nike Air Max 270',    149.99,  42,  75.00, 2, 5);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Adidas Hoodie',         64.99,  30,  28.00, 2, 5);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Merino Wool Sweater',   79.99,  15,  38.00, 2, 5);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Rain Jacket',           99.99,   5,  48.00, 2, 5);

-- Books
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Clean Code',            34.99,  90,  12.00, 3, 6);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Designing Data-Intensive Applications', 49.99, 55, 18.00, 3, 6);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('AI Agents with Java',   39.99,  40,  15.00, 3, 6);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('The Pragmatic Programmer', 44.99, 70, 16.00, 3, 6);

-- Sports
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Yoga Mat Pro',          39.99,  85,  15.00, 4, 5);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Kettlebell 20kg',        59.99,  25,  28.00, 4, 5);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Running Shoes Elite',   129.99,  18,  62.00, 4, 5);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Resistance Bands Set',   24.99,  95,   8.00, 4, 5);

-- Home & Kitchen
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Nespresso Vertuo',      179.99,  22,  95.00, 5, 6);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Instant Pot Duo',        99.99,  33,  48.00, 5, 6);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Vitamix Blender',       449.99,   3, 290.00, 5, 6);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Cast Iron Skillet',      49.99,  67,  18.00, 5, 6);
INSERT INTO products (name, price, stock, cost_price, category_id, supplier_id) VALUES ('Bamboo Cutting Board',   19.99, 110,   6.00, 5, 6);

-- Internal audit trail — a real table the assistant must not see (hidden entity)
INSERT INTO audit_logs (entity_name, action, performed_by, occurred_at) VALUES ('Product',  'PRICE_UPDATE', 'admin@acme.io',   TIMESTAMP '2026-06-01 09:14:00');
INSERT INTO audit_logs (entity_name, action, performed_by, occurred_at) VALUES ('Supplier', 'CREATE',       'buyer@acme.io',   TIMESTAMP '2026-06-02 11:30:00');
INSERT INTO audit_logs (entity_name, action, performed_by, occurred_at) VALUES ('Product',  'STOCK_ADJUST', 'wms@acme.io',     TIMESTAMP '2026-06-03 16:45:00');

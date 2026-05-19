-- Categories
INSERT INTO categories (name) VALUES ('Electronics');
INSERT INTO categories (name) VALUES ('Clothing');
INSERT INTO categories (name) VALUES ('Books');
INSERT INTO categories (name) VALUES ('Sports');
INSERT INTO categories (name) VALUES ('Home & Kitchen');

-- Products - Electronics
INSERT INTO products (name, price, stock, category_id) VALUES ('MacBook Pro 16"',   2499.99,  12, 1);
INSERT INTO products (name, price, stock, category_id) VALUES ('iPhone 15 Pro',     1199.99,  35, 1);
INSERT INTO products (name, price, stock, category_id) VALUES ('Sony WH-1000XM5',    379.99,  48, 1);
INSERT INTO products (name, price, stock, category_id) VALUES ('Samsung 4K Monitor',  449.99,  20, 1);
INSERT INTO products (name, price, stock, category_id) VALUES ('Logitech MX Keys',    109.99,  75, 1);
INSERT INTO products (name, price, stock, category_id) VALUES ('iPad Air',            749.99,   8, 1);
INSERT INTO products (name, price, stock, category_id) VALUES ('USB-C Hub 7-in-1',     49.99, 120, 1);

-- Products - Clothing
INSERT INTO products (name, price, stock, category_id) VALUES ('Levi 501 Jeans',      89.99,  60, 2);
INSERT INTO products (name, price, stock, category_id) VALUES ('Nike Air Max 270',    149.99,  42, 2);
INSERT INTO products (name, price, stock, category_id) VALUES ('Adidas Hoodie',        64.99,  30, 2);
INSERT INTO products (name, price, stock, category_id) VALUES ('Merino Wool Sweater',  79.99,  15, 2);
INSERT INTO products (name, price, stock, category_id) VALUES ('Rain Jacket',          99.99,   5, 2);

-- Products - Books
INSERT INTO products (name, price, stock, category_id) VALUES ('Clean Code',           34.99,  90, 3);
INSERT INTO products (name, price, stock, category_id) VALUES ('Designing Data-Intensive Applications', 49.99, 55, 3);
INSERT INTO products (name, price, stock, category_id) VALUES ('AI Agents with Java', 39.99,  40, 3);
INSERT INTO products (name, price, stock, category_id) VALUES ('The Pragmatic Programmer', 44.99, 70, 3);

-- Products - Sports
INSERT INTO products (name, price, stock, category_id) VALUES ('Yoga Mat Pro',         39.99,  85, 4);
INSERT INTO products (name, price, stock, category_id) VALUES ('Kettlebell 20kg',       59.99,  25, 4);
INSERT INTO products (name, price, stock, category_id) VALUES ('Running Shoes Elite',  129.99,  18, 4);
INSERT INTO products (name, price, stock, category_id) VALUES ('Resistance Bands Set',  24.99,  95, 4);

-- Products - Home & Kitchen
INSERT INTO products (name, price, stock, category_id) VALUES ('Nespresso Vertuo',    179.99,  22, 5);
INSERT INTO products (name, price, stock, category_id) VALUES ('Instant Pot Duo',      99.99,  33, 5);
INSERT INTO products (name, price, stock, category_id) VALUES ('Vitamix Blender',     449.99,   3, 5);
INSERT INTO products (name, price, stock, category_id) VALUES ('Cast Iron Skillet',    49.99,  67, 5);
INSERT INTO products (name, price, stock, category_id) VALUES ('Bamboo Cutting Board', 19.99, 110, 5);

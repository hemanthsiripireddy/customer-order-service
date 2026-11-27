CREATE TABLE customers (
    customer_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32)
);

CREATE UNIQUE INDEX ux_customers_username ON customers (username);

CREATE TABLE customer_addresses (
    address_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers (customer_id),
    address_line VARCHAR(512) NOT NULL,
    city VARCHAR(128) NOT NULL,
    state VARCHAR(128) NOT NULL,
    pincode VARCHAR(16) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX ix_customer_addresses_customer_id ON customer_addresses (customer_id);

CREATE TABLE orders (
    order_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers (customer_id),
    address_id UUID NOT NULL REFERENCES customer_addresses (address_id),
    total_price NUMERIC(19, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT ck_orders_total_price_non_negative CHECK (total_price >= 0),
    CONSTRAINT ck_orders_status CHECK (status IN (
        'PLACED', 'CANCELLED', 'REJECTED', 'DELIVERED', 'RETURNED', 'PARTIALLY_RETURNED'
    ))
);

CREATE INDEX ix_orders_customer_id ON orders (customer_id);
CREATE INDEX ix_orders_address_id ON orders (address_id);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (order_id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price_non_negative CHECK (unit_price >= 0)
);

CREATE INDEX ix_order_items_order_id ON order_items (order_id);

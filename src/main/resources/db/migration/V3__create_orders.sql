CREATE TABLE orders (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    channel           VARCHAR(30)    NOT NULL,
    external_order_id VARCHAR(100)   NOT NULL,
    status            VARCHAR(30)    NOT NULL,
    customer_name     VARCHAR(150)   NOT NULL,
    total_amount      DECIMAL(12, 2) NOT NULL,
    received_at       DATETIME(6)    NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    -- Garante no banco que o mesmo pedido de um canal não entra duas vezes.
    CONSTRAINT uk_orders_channel_external_id UNIQUE (channel, external_order_id)
);

CREATE INDEX ix_orders_status ON orders (status);

CREATE TABLE order_items (
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    order_id   BIGINT         NOT NULL,
    sku        VARCHAR(50)    NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price_positive CHECK (unit_price > 0)
);

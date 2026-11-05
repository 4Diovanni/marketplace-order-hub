CREATE TABLE products (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    sku            VARCHAR(50)    NOT NULL,
    name           VARCHAR(150)   NOT NULL,
    price          DECIMAL(12, 2) NOT NULL,
    stock_quantity INT            NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price_positive CHECK (price > 0),
    CONSTRAINT ck_products_stock_not_negative CHECK (stock_quantity >= 0)
);

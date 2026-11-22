-- Replenishing a SKU looks up the pending orders that contain it (issue #14).
CREATE INDEX ix_order_items_sku ON order_items (sku);

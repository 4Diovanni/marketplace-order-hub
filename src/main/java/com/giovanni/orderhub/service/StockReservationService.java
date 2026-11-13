package com.giovanni.orderhub.service;

import com.giovanni.orderhub.model.Product;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * The all-or-nothing stock rule, shared by new orders and by pending orders confirmed after a replenishment.
 *
 * <p>Runs inside the caller's transaction; the optimistic lock on {@link Product} catches concurrent changes.
 */
@Service
public class StockReservationService {

    /**
     * Reserves the stock of every SKU, or of none.
     *
     * @param quantityBySku total quantity needed per SKU
     * @param productBySku  products already loaded; a SKU missing from this map counts as having no stock
     * @return {@code true} if everything was reserved, {@code false} if nothing was
     */
    public boolean reserveAllOrNothing(Map<String, Integer> quantityBySku, Map<String, Product> productBySku) {
        if (!hasStockForAll(quantityBySku, productBySku)) {
            return false;
        }

        for (Map.Entry<String, Integer> entry : quantityBySku.entrySet()) {
            Product product = productBySku.get(entry.getKey());
            product.adjustStock(-entry.getValue());
        }
        return true;
    }

    private boolean hasStockForAll(Map<String, Integer> quantityBySku, Map<String, Product> productBySku) {
        for (Map.Entry<String, Integer> entry : quantityBySku.entrySet()) {
            Product product = productBySku.get(entry.getKey());
            if (product == null || !product.hasStockFor(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}

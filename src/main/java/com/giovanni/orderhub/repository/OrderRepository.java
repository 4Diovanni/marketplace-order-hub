package com.giovanni.orderhub.repository;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.model.Order;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Optional filters: {@code null} status or channel means "any". */
    @Query("""
            select o from Order o
            where (:status is null or o.status = :status)
              and (:channel is null or o.channel = :channel)
            """)
    Page<Order> search(@Param("status") OrderStatus status, @Param("channel") Channel channel, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsByChannelAndExternalOrderId(Channel channel, String externalOrderId);

    /** Ids of the orders with the given status that have at least one line for the SKU, oldest first. */
    @Query("""
            select o.id from Order o
            where o.status = :status
              and exists (select 1 from OrderItem i where i.order = o and i.sku = :sku)
            order by o.receivedAt, o.id
            """)
    List<Long> findIdsByStatusAndItemSkuInArrivalOrder(@Param("status") OrderStatus status, @Param("sku") String sku);

    /**
     * Loads the order with its items and locks the row until the transaction ends ({@code SELECT ... FOR UPDATE}),
     * so two stock replenishments cannot confirm the same order at the same time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    Optional<Order> findLockedWithItemsById(Long id);
}

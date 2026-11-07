package com.giovanni.orderhub.repository;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.model.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

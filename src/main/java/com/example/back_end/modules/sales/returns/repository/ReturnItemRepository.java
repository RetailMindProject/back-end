package com.example.back_end.modules.sales.returns.repository;

import com.example.back_end.modules.sales.returns.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

    /**
     * Sum already returned quantity for an original order item across ALL return orders
     * belonging to a given original order (orders.parent_order_id = originalOrderId)
     * where return order status='RETURNED'.
     */
    @Query("""
            SELECT COALESCE(SUM(ri.returnedQty), 0)
            FROM ReturnItem ri
            JOIN ri.returnOrder ro
            WHERE ri.originalOrderItem.id = :originalOrderItemId
              AND ro.parentOrderId = :originalOrderId
              AND ro.status = com.example.back_end.modules.sales.order.entity.Order.OrderStatus.RETURNED
            """)
    BigDecimal sumReturnedQty(@Param("originalOrderId") Long originalOrderId,
                             @Param("originalOrderItemId") Long originalOrderItemId);

    @Query("SELECT ri FROM ReturnItem ri WHERE ri.returnOrder.id = :returnOrderId ORDER BY ri.id")
    List<ReturnItem> findByReturnOrderId(@Param("returnOrderId") Long returnOrderId);

    @Query("SELECT COUNT(ri) FROM ReturnItem ri WHERE ri.returnOrder.id = :returnOrderId")
    long countByReturnOrderId(@Param("returnOrderId") Long returnOrderId);
}

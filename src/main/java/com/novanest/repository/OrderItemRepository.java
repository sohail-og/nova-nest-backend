package com.novanest.repository;

import com.novanest.model.Order;
import com.novanest.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder(Order order);
    List<OrderItem> findByOrder_OrderId(String orderId);
}

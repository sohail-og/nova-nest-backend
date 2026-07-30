package com.novanest.repository;

import com.novanest.model.CartItem;
import com.novanest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndProduct_Id(User user, Integer productId);
    void deleteByUser(User user);
}

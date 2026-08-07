package com.novanest.repository;

import com.novanest.model.WishlistItem;
import com.novanest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Integer> {
    List<WishlistItem> findByUser(User user);

    Optional<WishlistItem> findByUserAndProduct_Id(User user, Integer productId);

    void deleteByUser(User user);
}

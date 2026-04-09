package com.example.demo.repository;

import com.example.demo.model.CartItem;
import com.example.demo.model.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCart(ShoppingCart cart);
}

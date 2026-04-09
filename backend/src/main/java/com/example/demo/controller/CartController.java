package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.requestDto.AddToCartRequest;
import com.example.demo.responseDto.CartResponseDto;
import com.example.demo.responseDto.LoggedInMemberDto;
import com.example.demo.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @AuthenticationPrincipal LoggedInMemberDto user,
            @RequestBody AddToCartRequest request) {
        try {
            cartService.addItemToCart(user.getId(), request.getItineraryId());
            return ResponseEntity.ok(java.util.Map.of("message", "Item added to cart successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart(@AuthenticationPrincipal LoggedInMemberDto user) {
        try {
            CartResponseDto cart = cartService.getCartDetails(user.getId());
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<?> removeFromCart(
            @AuthenticationPrincipal LoggedInMemberDto user,
            @PathVariable Integer cartItemId) {
        try {

            cartService.removeItemFromCart(user.getId(), cartItemId);
            return ResponseEntity.ok(java.util.Map.of("message", "Item removed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@AuthenticationPrincipal LoggedInMemberDto user) {
        try {
            cartService.checkoutCart(user.getId());
            return ResponseEntity.ok(java.util.Map.of("message", "Cart checked out successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/purchased-ids")
    public ResponseEntity<?> getPurchasedItems(@AuthenticationPrincipal LoggedInMemberDto user) {
        try {
            return ResponseEntity.ok(cartService.getPurchasedItineraryIds(user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}

package com.example.demo.controller;

import com.example.demo.linepayDto.LinePayConfirmDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.PaymentService;
import com.example.demo.responseDto.LoggedInMemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthService authService;

    /**
     * 向 LINE Pay 發起支付請求
     * Frontend sends: { "packageId": 1 }
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestPayment(@RequestBody Map<String, Integer> request) {
        try {
            Integer packageId = request.get("packageId");
            if (packageId == null) {
                return ResponseEntity.badRequest().body("packageId is required");
            }

            LoggedInMemberDto user = authService.getLoggedInUser();
            String paymentUrl = paymentService.initiatePayment(user.getId(), packageId);

            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
        } catch (Exception e) {
            log.error("Payment request failed", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * LINE Pay 支付成功後，前端呼叫此 API 進行確認
     * Frontend sends: { "transactionId": "...", "orderId": "..." }
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody LinePayConfirmDto confirmRequest) {
        try {
            paymentService.confirmPayment(confirmRequest.getTransactionId(), confirmRequest.getOrderId());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment confirmed"));
        } catch (Exception e) {
            log.error("Payment confirmation failed", e);
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * 取得當前使用者的交易紀錄
     */
    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory() {
        try {
            LoggedInMemberDto user = authService.getLoggedInUser();
            return ResponseEntity.ok(paymentService.getUserTransactions(user.getId()));
        } catch (Exception e) {
            log.error("Failed to fetch transaction history", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}

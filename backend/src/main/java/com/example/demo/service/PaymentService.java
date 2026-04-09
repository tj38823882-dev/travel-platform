package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.demo.linepayDto.LinePayConfirmDto;
import com.example.demo.linepayDto.LinePayRequestDto;
import com.example.demo.model.PointPackage;
import com.example.demo.model.PointTransaction;
import com.example.demo.model.TopupOrder;
import com.example.demo.model.User;
import com.example.demo.repository.PointPackageRepository;
import com.example.demo.repository.PointTransactionRepository;
import com.example.demo.repository.TopupOrderRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.responseDto.PointTransactionResponseDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

        private final TopupOrderRepository topupOrderRepository;
        private final PointPackageRepository packageRepository;
        private final UserRepository userRepository;
        private final PointTransactionRepository pointTransactionRepository;
        private final ObjectMapper objectMapper;
        private final RestTemplate restTemplate = new RestTemplate();

        @Value("${linepay.channel.id}")
        private String channelId;

        @Value("${linepay.channel.secret}")
        private String channelSecret;

        @Value("${linepay.sandbox.request-url:https://sandbox-api-pay.line.me/v3/payments/request}")
        private String requestUrl;

        @Value("${linepay.sandbox.base-url:https://sandbox-api-pay.line.me}")
        private String baseUrl;

        @Value("${frontend.payment.confirm-url:http://localhost:5173/payment/confirm}")
        private String confirmRedirectUrl;

        @Value("${frontend.payment.cancel-url:http://localhost:5173/payment/cancel}")
        private String cancelRedirectUrl;

        @Transactional
        public String initiatePayment(Integer userId, Integer packageId) throws Exception {
                // 1. 取得點數包資訊
                PointPackage pkg = packageRepository.findById(packageId)
                                .orElseThrow(() -> new RuntimeException("找不到編號為 " + packageId + " 的點數方案"));

                // 2. 取得使用者資訊
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("找不到編號為 " + userId + " 的使用者"));

                // 3. 在資料庫建立一筆 TopupOrder (Status: 0 = Pending)
                TopupOrder order = new TopupOrder();

                // 使用 setter 設定實體物件，JPA 會自動處理外鍵 ID
                order.setUser(user);
                order.setPointPackage(pkg);
                order.setAmountCash(pkg.getPriceCash());
                order.setPointsGranted(pkg.getPointsAmount());
                order.setPaymentStatus(0); // 待付款
                order = topupOrderRepository.save(order);

                // 4. 準備 LINE Pay 請求 Body
                String customOrderId = "ORDER_" + order.getId() + "_" + UUID.randomUUID().toString().substring(0, 8);

                LinePayRequestDto requestBody = LinePayRequestDto.builder()
                                .amount(order.getAmountCash())
                                .currency("TWD")
                                .orderId(customOrderId)
                                .packages(List.of(LinePayRequestDto.PackageDto.builder()
                                                .id("PKG_" + pkg.getId())
                                                .amount(order.getAmountCash())
                                                .name("點數儲值 - " + pkg.getPointsAmount() + " 點")
                                                .products(List.of(LinePayRequestDto.ProductDto.builder()
                                                                .name(pkg.getPointsAmount() + " 點數")
                                                                .quantity(1)
                                                                .price(pkg.getPriceCash())
                                                                .build()))
                                                .build()))
                                .redirectUrls(LinePayRequestDto.RedirectUrlsDto.builder()
                                                .confirmUrl(confirmRedirectUrl) // 用戶付完跳轉回 Vue
                                                .cancelUrl(cancelRedirectUrl)
                                                .build())
                                .build();

                // 4. 加密 Header 處理
                String bodyJson = objectMapper.writeValueAsString(requestBody);
                String nonce = UUID.randomUUID().toString();
                String uri = "/v3/payments/request";

                // 簽名公式: Base64(HMAC-SHA256(Secret, Secret + URI + Body + Nonce))
                String signature = Base64.getEncoder().encodeToString(
                                new HmacUtils(HmacAlgorithms.HMAC_SHA_256, channelSecret)
                                                .hmac(channelSecret + uri + bodyJson + nonce));

                // 5. 設定 HTTP Headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-LINE-ChannelId", channelId);
                headers.set("X-LINE-Authorization-Nonce", nonce);
                headers.set("X-LINE-Authorization", signature);

                // 6. 發送請求至 LINE Pay
                HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
                ResponseEntity<JsonNode> response = restTemplate.postForEntity(requestUrl, entity, JsonNode.class);

                JsonNode resBody = response.getBody();
                if (resBody != null && "0000".equals(resBody.get("returnCode").asText())) {
                        // 成功：取得 LINE Pay 付款網址
                        return resBody.get("info").get("paymentUrl").get("web").asText();
                } else {
                        log.error("LINE Pay 請求失敗: {}", resBody);
                        throw new RuntimeException(
                                        "LINE Pay 支付發起失敗: " + (resBody != null ? resBody.get("returnMessage").asText()
                                                        : "未知錯誤"));
                }
        }

        @Transactional
        public void confirmPayment(String transactionId, String orderId) throws Exception {
                // 1. 根據 orderId 找到該筆訂單
                Integer dbOrderId = Integer.parseInt(orderId.split("_")[1]);
                TopupOrder order = topupOrderRepository.findById(dbOrderId)
                                .orElseThrow(() -> new RuntimeException("找不到對應訂單"));

                if (order.getPaymentStatus() != 0) {
                        throw new RuntimeException("訂單狀態不正確，可能已處理過");
                }

                // 2. 準備 Confirm API 的請求內容
                LinePayConfirmDto confirmDto = new LinePayConfirmDto();
                confirmDto.setAmount(order.getAmountCash());
                confirmDto.setCurrency("TWD");
                String bodyJson = objectMapper.writeValueAsString(confirmDto);

                // 3. 準備 Header (注意：URI 必須包含 transactionId)
                String nonce = UUID.randomUUID().toString();
                String uri = "/v3/payments/" + transactionId + "/confirm";
                String signature = Base64.getEncoder().encodeToString(
                                new HmacUtils(HmacAlgorithms.HMAC_SHA_256, channelSecret)
                                                .hmac(channelSecret + uri + bodyJson + nonce));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-LINE-ChannelId", channelId);
                headers.set("X-LINE-Authorization-Nonce", nonce);
                headers.set("X-LINE-Authorization", signature);

                // 4. 發送請求
                String confirmUrl = baseUrl + "/v3/payments/" + transactionId + "/confirm";
                HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
                ResponseEntity<JsonNode> response = restTemplate.postForEntity(confirmUrl, entity, JsonNode.class);

                JsonNode resBody = response.getBody();

                // 5. 處理回傳結果
                if (resBody != null && "0000".equals(resBody.get("returnCode").asText())) {
                        // --- 成功：執行業務邏輯 ---

                        // A. 更新訂單狀態為 1 (Completed)
                        order.setPaymentStatus(1);
                        order.setTransactionId(transactionId); // 儲存 LINE Pay 給的交易號碼
                        topupOrderRepository.save(order);

                        // B. 寫入 PointTransaction (點數流水帳)
                        PointTransaction pt = new PointTransaction();
                        pt.setUser(order.getUser());
                        pt.setTopupOrder(order);
                        pt.setType("topup");
                        pt.setPoints(order.getPointsGranted());
                        pt.setDescription("儲值點數: " + order.getPointsGranted() + " 點");
                        pointTransactionRepository.save(pt);

                        // C. (選擇性) 更新 User 表的總餘額
                        User user = order.getUser();
                        user.setPoints(user.getPoints() + order.getPointsGranted());
                        userRepository.save(user);

                        log.info("訂單 {} 付款成功，transactionId: {}", orderId, transactionId);
                } else {
                        // --- 失敗 ---
                        order.setPaymentStatus(2); // Failed
                        topupOrderRepository.save(order);
                        log.error("LINE Pay Confirm 失敗: {}", resBody);
                        throw new RuntimeException("付款確認失敗：" + resBody.get("returnMessage").asText());
                }
        }

        @Transactional(readOnly = true)
        public List<PointTransactionResponseDto> getUserTransactions(Integer userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<PointTransaction> transactions = pointTransactionRepository.findByUserOrderByCreatedAtDesc(user);

                return transactions.stream().map(pt -> {
                        TopupOrder order = pt.getTopupOrder();
                        return PointTransactionResponseDto.builder()
                                        .id(pt.getId())
                                        .date(pt.getCreatedAt())
                                        .points(pt.getPoints())
                                        .type(pt.getType())
                                        .price(order != null ? order.getAmountCash() : null)
                                        .method(order != null ? "LINE Pay" : "-")
                                        .status("已完成")
                                        .orderId(order != null ? "TXN-" + order.getId() : "P-" + pt.getId())
                                        .description(pt.getDescription() != null ? pt.getDescription()
                                                        : (order != null ? "點數儲值 - " + order.getPointsGranted() + " 點"
                                                                        : "點數異動"))
                                        .build();
                }).toList();
        }
}

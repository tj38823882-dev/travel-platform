package com.example.demo.linepayDto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class LinePayRequestDto {
    private Integer amount;
    private String currency;
    private String orderId;
    private List<PackageDto> packages;
    private RedirectUrlsDto redirectUrls;

    @Data
    @Builder
    public static class PackageDto {
        private String id;
        private Integer amount;
        private String name;
        private List<ProductDto> products;
    }

    @Data
    @Builder
    public static class ProductDto {
        private String name;
        private Integer quantity;
        private Integer price;
    }

    @Data
    @Builder
    public static class RedirectUrlsDto {
        private String confirmUrl;
        private String cancelUrl;
    }
}
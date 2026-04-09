package com.example.demo.linepayDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinePayConfirmDto {
    private Integer amount;
    private String currency;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String transactionId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String orderId;
}
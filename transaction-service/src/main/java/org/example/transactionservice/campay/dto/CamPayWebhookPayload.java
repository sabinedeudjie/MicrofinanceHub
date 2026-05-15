package org.example.transactionservice.campay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CamPayWebhookPayload {

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("status")
    private String status;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("code")
    private String code;

    @JsonProperty("external_reference")
    private String externalReference;

    @JsonProperty("description")
    private String description;

    public boolean isSuccessful() { return "SUCCESSFUL".equalsIgnoreCase(status); }
    public boolean isFailed()     { return "FAILED".equalsIgnoreCase(status); }
}

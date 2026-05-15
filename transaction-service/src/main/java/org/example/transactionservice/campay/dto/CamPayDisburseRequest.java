package org.example.transactionservice.campay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CamPayDisburseRequest {

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("to")
    private String to;

    @JsonProperty("description")
    private String description;

    @JsonProperty("external_reference")
    private String externalReference;
}

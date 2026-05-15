package org.example.transactionservice.campay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CamPayDisburseResponse {

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("status")
    private String status;

    @JsonProperty("operator")
    private String operator;
}

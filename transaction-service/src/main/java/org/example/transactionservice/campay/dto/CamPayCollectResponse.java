package org.example.transactionservice.campay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CamPayCollectResponse {

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("ussd_code")
    private String ussdCode;

    @JsonProperty("status")
    private String status;
}

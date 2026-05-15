package org.example.repaymentservice.client;

import org.example.repaymentservice.dto.request.RemboursementTxRequest;
import org.example.repaymentservice.dto.response.TransactionTxResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "TRANSACTION-SERVICE", url = "${transaction.service.url:http://localhost:8088}")
public interface TransactionServiceClient {

    @PostMapping("/api/transactions/remboursement-pret")
    TransactionTxResponse enregistrerRemboursement(
            @RequestBody RemboursementTxRequest request,
            @RequestHeader("Authorization") String authorization
    );
}

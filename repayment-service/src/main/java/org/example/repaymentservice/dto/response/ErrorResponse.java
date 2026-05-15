package org.example.repaymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String code;
    private String message;
    private String path;
}


//  org.example.repaymentservice.dto.response;

//  java.time.LocalDateTime;

//  lombok.Builder;
//  lombok.Data;

// 
// 
//  class ErrorResponse {
//      LocalDateTime timestamp;
//      int status;
//      String code;
//      String message;
//      String path;
// 

package com.example.account.exception.transaction;

import com.example.account.type.ErrorCode;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionException extends RuntimeException {
    private ErrorCode errorCode;
    private String errorMessage;

    public TransactionException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }
}
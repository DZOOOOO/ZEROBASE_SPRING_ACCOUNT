package com.example.account.exception;

import com.example.account.dto.error.ErrorResponse;
import com.example.account.exception.account.AccountException;
import com.example.account.exception.transaction.TransactionException;
import com.example.account.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 전역 예외처리 클래스
public class GlobalException {

    @ExceptionHandler(AccountException.class)
    public ErrorResponse handleAccountException(AccountException e) {
        log.error("{} is occurred", e.getErrorCode());
        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(TransactionException.class)
    public ErrorResponse handleTransactionException(TransactionException e) {
        log.error("{} is occurred", e.getErrorCode());
        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    public ErrorResponse handleNullPointerException(NullPointerException e) {
        log.error("NullPointerException is occurred.", e);
        return new ErrorResponse(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getDescription());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException is occurred.", e);
        return new ErrorResponse(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getDescription());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException is occurred.", e);
        return new ErrorResponse(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getDescription());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e) {
        log.error("Exception is occurred.", e);
        return new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getDescription());
    }

}

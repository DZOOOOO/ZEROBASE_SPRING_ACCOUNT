package com.example.account.controller.transaction;

import com.example.account.aop.AccountLock;
import com.example.account.dto.transaction.CancelBalance;
import com.example.account.dto.transaction.QueryTransactionResponse;
import com.example.account.dto.transaction.UseBalance;
import com.example.account.exception.account.AccountException;
import com.example.account.service.transaction.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    // 거래 생성 API
    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(@Valid @RequestBody UseBalance.Request request) throws InterruptedException {

        try {
            // 성공건 저장
            Thread.sleep(3000L);
            return UseBalance.Response.from(
                    transactionService.useBalance(
                            request.getUserId(),
                            request.getAccountNumber(),
                            request.getAmount()));
        } catch (AccountException e) {
            // 실패건 저장
            log.error("Failed to use balance. ");

            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }

    }

    // 거래 취소 API
    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(@Valid @RequestBody CancelBalance.Request request) {
        try {
            return CancelBalance.Response.from(
                    transactionService.cancelBalance(request.getTransactionId(),
                            request.getAccountNumber(), request.getAmount())
            );
        } catch (AccountException e) {
            log.error("Failed to use balance. ");

            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e;
        }
    }

    // 잔액 사용 확인 API
    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(@PathVariable("transactionId") String transactionId) {
        return QueryTransactionResponse.from(transactionService.queryTransaction(transactionId));
    }

}
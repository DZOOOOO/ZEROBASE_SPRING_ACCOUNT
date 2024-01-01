package com.example.account.controller.transaction;

import com.example.account.dto.transaction.CancelBalance;
import com.example.account.dto.transaction.TransactionDto;
import com.example.account.dto.transaction.UseBalance;
import com.example.account.exception.account.AccountException;
import com.example.account.exception.transaction.TransactionException;
import com.example.account.service.transaction.TransactionService;
import com.example.account.type.ErrorCode;
import com.example.account.type.transaction.TransactionResultType;
import com.example.account.type.transaction.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("잔액사용 - 성공")
    void successUseBalance() throws Exception {
        // given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1000000000")
                        .transactedAt(LocalDateTime.now())
                        .amount(12345L)
                        .transactionId("transactionId")
                        .transactionResultType(TransactionResultType.S)
                        .build());
        // when
        // then
        mockMvc.perform(post("/transaction/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UseBalance.Request.builder()
                                        .userId(1L)
                                        .accountNumber("2000000000")
                                        .amount(3000L)
                                        .build())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(12345L));
    }

    @Test
    @DisplayName("잔액사용 - 실패 - 계좌가 없는 경우")
    void failUseBalance_NotAccount() throws Exception {
        // given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND,
                        ErrorCode.ACCOUNT_NOT_FOUND.getDescription()));

        // when
        // then
        mockMvc.perform(post("/transaction/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UseBalance.Request.builder()
                                        .userId(1L)
                                        .accountNumber("2000000000")
                                        .amount(3000L)
                                        .build())))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("계좌가 없습니다."));
    }

    @Test
    @DisplayName("잔액사용 - 실패 - 사용 금액이 잔액 보다 큰 경우")
    void failUseBalance() throws Exception {
        // given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willThrow(new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE,
                        ErrorCode.AMOUNT_EXCEED_BALANCE.getDescription()));

        // when
        // then
        mockMvc.perform(post("/transaction/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UseBalance.Request.builder()
                                        .userId(1L)
                                        .accountNumber("2000000000")
                                        .amount(3000L)
                                        .build())))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("AMOUNT_EXCEED_BALANCE"))
                .andExpect(jsonPath("$.errorMessage").value("거래 금액이 계좌 잔액보다 큽니다."));
    }

    @Test
    @DisplayName("거래취소 - 성공")
    void successCancelBalance() throws Exception {
        // given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1000000000")
                        .transactedAt(LocalDateTime.now())
                        .amount(54321L)
                        .transactionId("transactionIdForCancel")
                        .transactionResultType(TransactionResultType.S)
                        .build());
        // when
        // then
        mockMvc.perform(post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CancelBalance.Request.builder()
                                        .transactionId("transactionId")
                                        .accountNumber("2000000000")
                                        .amount(3000L)
                                        .build())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionIdForCancel"))
                .andExpect(jsonPath("$.amount").value(54321L));
    }

    @Test
    @DisplayName("거래취소 - 실패 - 거래 유효기간을 지난 경우")
    void failCancelBalance_Not() throws Exception {
        // given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
                .willThrow(new TransactionException(ErrorCode.TOO_OLD_ORDER_TO_CANCEL,
                        ErrorCode.TOO_OLD_ORDER_TO_CANCEL.getDescription()));
        // when
        // then
        mockMvc.perform(post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CancelBalance.Request.builder()
                                        .transactionId("transactionId")
                                        .accountNumber("2000000000")
                                        .amount(3000L)
                                        .build())))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("TOO_OLD_ORDER_TO_CANCEL"))
                .andExpect(jsonPath("$.errorMessage").value("1년이 지난 거래는 취소가 불가능합니다."));
    }

    @Test
    @DisplayName("잔액 사용확인 - 성공")
    void successQueryTransaction() throws Exception {
        // given
        given(transactionService.queryTransaction(anyString()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1000000000")
                        .transactionType(TransactionType.USE)
                        .transactedAt(LocalDateTime.now())
                        .amount(54321L)
                        .transactionId("transactionId")
                        .transactionResultType(TransactionResultType.S)
                        .build());

        // when
        // then
        mockMvc.perform(get("/transaction/12345"))
                .andDo(print())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(54321L));
    }

    @Test
    @DisplayName("잔액 사용확인 - 실패 - 계좌가 없는 경우")
    void failQueryTransaction_NotAccount() throws Exception {
        // given
        given(transactionService.queryTransaction(anyString()))
                .willThrow(new TransactionException(ErrorCode.ACCOUNT_NOT_FOUND,
                        ErrorCode.ACCOUNT_NOT_FOUND.getDescription()));

        // when
        // then
        mockMvc.perform(get("/transaction/12345"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("계좌가 없습니다."));
    }

    @Test
    @DisplayName("잔액 사용확인 - 실패 - 계좌가 해지된 경우")
    void failQueryTransaction_Unregistered() throws Exception {
        // given
        given(transactionService.queryTransaction(anyString()))
                .willThrow(new TransactionException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED,
                        ErrorCode.ACCOUNT_ALREADY_UNREGISTERED.getDescription()));

        // when
        // then
        mockMvc.perform(get("/transaction/12345"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_ALREADY_UNREGISTERED"))
                .andExpect(jsonPath("$.errorMessage").value("계좌가 이미 해지되었습니다."));
    }

}
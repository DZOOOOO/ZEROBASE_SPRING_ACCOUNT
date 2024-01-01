package com.example.account.controller.account;

import com.example.account.domain.account.Account;
import com.example.account.dto.account.AccountDto;
import com.example.account.dto.account.CreateAccount;
import com.example.account.dto.account.DeleteAccount;
import com.example.account.exception.account.AccountException;
import com.example.account.service.account.AccountService;
import com.example.account.type.ErrorCode;
import com.example.account.type.account.AccountStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @MockBean
    private AccountService accountService;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /account (성공 케이스)")
    void successCreateAccount() throws Exception {
        // given
        given(accountService.createdAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        // when
        // then
        mockMvc.perform(post("/account")
                        // header
                        .contentType(MediaType.APPLICATION_JSON)
                        // body
                        .content(objectMapper.writeValueAsString(
                                CreateAccount.Request.builder()
                                        .userId(1L)
                                        .initialBalance(100L)
                                        .build()
                        )))
                // 기대값
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                // http 요청 프린트
                .andDo(print());
    }

    @Test
    @DisplayName("POST /account (실패 케이스) - 사용자가 초기 잔액을 잘못 요청 한 경우")
    void failCreateAccount() throws Exception {
        // given
        given(accountService.createdAccount(anyLong(), anyLong()))
                .willThrow(new AccountException(ErrorCode.INVALID_REQUEST,
                        ErrorCode.INVALID_REQUEST.getDescription()));
        // when
        // then
        mockMvc.perform(post("/account")
                        // header
                        .contentType(MediaType.APPLICATION_JSON)
                        // body
                        .content(objectMapper.writeValueAsString(
                                CreateAccount.Request.builder()
                                        .userId(1L)
                                        .initialBalance(-100L)
                                        .build()
                        )))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errorMessage").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("DELETE /account (성공 케이스)")
    void successDeleteAccount() throws Exception {
        // given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        // when
        // then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                DeleteAccount.Request.builder()
                                        .userId(1L)
                                        .accountNumber("0987654321")
                                        .build()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }

    @Test
    @DisplayName("DELETE /account (실패 케이스) - 유저가 없는 경우")
    void failDeleteAccount_NotUser() throws Exception {
        // given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willThrow(new AccountException(ErrorCode.USER_NOT_FOUND,
                        ErrorCode.USER_NOT_FOUND.getDescription()));
        // when
        // then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                DeleteAccount.Request.builder()
                                        .userId(123L)
                                        .accountNumber("0987654321")
                                        .build()
                        )))
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("사용자가 없습니다."))
                .andDo(print());
    }
    @Test
    @DisplayName("DELETE /account (실패 케이스) - 계좌가 없는 경우")
    void failDeleteAccount_NotAccount() throws Exception {
        // given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND,
                        ErrorCode.ACCOUNT_NOT_FOUND.getDescription()));
        // when
        // then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                DeleteAccount.Request.builder()
                                        .userId(1L)
                                        .accountNumber("0987654321")
                                        .build()
                        )))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("계좌가 없습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("GET /account?user_id=1 (성공 케이스)")
    void successGetAccountsByUserId() throws Exception {
        // given
        List<AccountDto> accountDtos = Arrays.asList(
                AccountDto.builder()
                        .accountNumber("1234567890")
                        .balance(1000L).build(),
                AccountDto.builder()
                        .accountNumber("1111111111")
                        .balance(2000L).build(),
                AccountDto.builder()
                        .accountNumber("2222222222")
                        .balance(3000L).build()
        );

        given(accountService.getAccountsByUserId(anyLong()))
                .willReturn(accountDtos);

        // when
        // then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber").value("1234567890"))
                .andExpect(jsonPath("$[0].balance").value(1000))
                .andExpect(jsonPath("$[1].accountNumber").value("1111111111"))
                .andExpect(jsonPath("$[1].balance").value(2000))
                .andExpect(jsonPath("$[2].accountNumber").value("2222222222"))
                .andExpect(jsonPath("$[2].balance").value(3000));
    }

    @Test
    @DisplayName("GET /account?user_id=1 (실패 케이스) - 사용자가 없는 경우")
    void failGetAccountsByUserId() throws Exception {
        // given
        given(accountService.getAccountsByUserId(anyLong()))
                .willThrow(new AccountException(ErrorCode.USER_NOT_FOUND,
                        ErrorCode.USER_NOT_FOUND.getDescription()));

        // when
        // then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("사용자가 없습니다."));
    }

    @Test
    @DisplayName("유저 아이디로 계좌 조회")
    void successGetAccount() throws Exception {
        // given --> service 에서 생성
        given(accountService.getAccount(anyLong()))
                .willReturn(Account.builder()
                        .accountNumber("3456")
                        .accountStatus(AccountStatus.IN_USE)
                        .build());
        // when
        // then --> Controller 에서 요청
        mockMvc.perform(get("/account/876"))
                .andDo(print())
                .andExpect(jsonPath("$.accountNumber").value("3456"))
                .andExpect(jsonPath("$.accountStatus").value("IN_USE"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유저 아이디로 계좌 조회 - 실패")
    void failGetAccount() throws Exception {
        // given
        given(accountService.getAccount(anyLong()))
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        // when
        // then
        mockMvc.perform(get("/account/876"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("계좌가 없습니다."));
    }
}
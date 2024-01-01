package com.example.account.service;

import com.example.account.domain.account.Account;
import com.example.account.domain.account.AccountUser;
import com.example.account.dto.account.AccountDto;
import com.example.account.exception.account.AccountException;
import com.example.account.repository.account.AccountRepository;
import com.example.account.repository.account.AccountUserRepository;
import com.example.account.service.account.AccountService;
import com.example.account.type.account.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// mockito 선언.
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock // 가짜로 만들어서 주입.
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks // @Mock 주입.
    private AccountService accountService;

    @Test
    @DisplayName("계좌생성 성공")
    void createAccountSuccess() {
        // given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012").build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto = accountService.createdAccount(1L, 1000L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        // 계좌 번호가 무작위로 들어 오기 때문에 새롭게 만든 메서드에서는 10자리 정수인지만 체크
        assertEquals(10, captor.getValue().getAccountNumber().length());
        // assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌등록이 처음인 경우")
    void createFirstAccount() {
        // given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(15L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());

        // 실제 저장값은 captor 가 가지고 있다.
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto = accountService.createdAccount(1L, 1000L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        // 계좌 번호가 무작위로 들어 오기 때문에 새롭게 만든 메서드에서는 10자리 정수인지만 체크
         assertEquals(10, captor.getValue().getAccountNumber().length());
        // assertEquals("1000000013", captor.getValue().getAccountNumber());

    }

    @Test
    @DisplayName("사용자가 없는 경우 -> 계좌생성 안됨.")
    void createAccount_UserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createdAccount(1L, 1000L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 생성 10개 초과인 경우 -> 계좌생성 안됨.")
    void createAccount_maxAccountIs10() {
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(15L);

        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createdAccount(1L, 1000L));

        // then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌해지 성공")
    void deleteAccountSuccess() {
        // given
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("사용자가 없는 경우 -> 계좌해지 실패")
    void deleteAccount_userNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));
        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 - 계좌 해지 실패")
    void deleteAccount_AccountNotFound() {
        // given
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(12L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class, () -> {
            accountService.deleteAccount(12L, "1234567890");
        });

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 -> 계좌해지 실패")
    void deleteAccountFailed_userUnMatch() {
        // given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(1L);

        AccountUser harry = AccountUser.builder()
                .name("Harry")
                .build();
        harry.setId(13L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(any()))
                .willReturn(Optional.ofNullable(Account.builder()
                        .accountUser(harry)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액이 있으면 -> 계좌해지 실패")
    void deleteAccountFailed_balanceNotEmpty() {
        // given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(any()))
                .willReturn(Optional.ofNullable(Account.builder()
                        .accountUser(pobi)
                        .balance(100L)
                        .accountNumber("1000000012")
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 해지된 계좌 -> 계좌해지 실패")
    void deleteAccountFailed_alreadyUnregistered() {
        // given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(any()))
                .willReturn(Optional.ofNullable(Account.builder()
                        .accountUser(pobi)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("유저 아이디로 계좌를 불러오는 경우.")
    void successGetAccountsByUserId() {
        // given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(1L);

        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("1111111111")
                        .balance(1000L).build(),
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("2222222222")
                        .balance(2000L).build(),
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("3333333333")
                        .balance(3000L).build()
        );

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        // when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

        // then
        assertEquals(3, accountDtos.size());
        assertEquals("1111111111", accounts.get(0).getAccountNumber());
        assertEquals(1000, accounts.get(0).getBalance());
        assertEquals("2222222222", accounts.get(1).getAccountNumber());
        assertEquals(2000, accounts.get(1).getBalance());
        assertEquals("3333333333", accounts.get(2).getAccountNumber());
        assertEquals(3000, accounts.get(2).getBalance());

    }

    @Test
    @DisplayName("사용자가 없는데 계좌를 불러오는 경우")
    void failedToGetAccounts() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
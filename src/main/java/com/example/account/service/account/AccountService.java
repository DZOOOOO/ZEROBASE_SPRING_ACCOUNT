package com.example.account.service.account;

import com.example.account.domain.account.Account;
import com.example.account.domain.account.AccountUser;
import com.example.account.dto.account.AccountDto;
import com.example.account.exception.account.AccountException;
import com.example.account.repository.account.AccountRepository;
import com.example.account.repository.account.AccountUserRepository;
import com.example.account.type.account.AccountStatus;
import com.example.account.type.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     * 사용자 존재 체크
     * 계좌 번호 생성
     * 계좌 저장 -> 정보 넘김
     */
    @Transactional
    public AccountDto createdAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = getAccountUser(userId);

        validateCreateAccount(accountUser);

        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
                .orElse("1000000000");

        /////////////////////////////////////// 계좌번호 무작위 메서드 적용 ///////////////////////////////////////
        String newAccountNumberV2 = checkAccountNumber(createAccountNumber(userId));

        return AccountDto.fromEntity(
                accountRepository.save(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(AccountStatus.IN_USE)
                        .accountNumber(newAccountNumberV2)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build())
        );
    }

    // 계좌중복 체크 메서드
    private String checkAccountNumber(String accountNumber) {
        String result = "";
        // 중복이 없을 때 까지 반복을 돌린다.
        int count = 0;

        while (true) {
            Optional<Account> find = accountRepository.findByAccountNumber(accountNumber);
            if (find.isEmpty()) {
                result = accountNumber;
                break;
            } else {
                count += 1;
            }

            // 1000번 이상 조회에도 계좌 중복이 계속되는 경우 예외처리
            if (count > 1000) {
                throw new AccountException(ErrorCode.INVALID_REQUEST,
                        ErrorCode.INVALID_REQUEST.getDescription());
            }
        }
        return result;
    }

    // 계좌 무작위 번호 생성 메서드
    private static String createAccountNumber(Long id) {
        StringBuilder sb = new StringBuilder();
        int[] nums = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int tmp = 0;

        for (int i = 0; i < 9; i++) {
            int index = (int) (Math.random() * 10);
            tmp = nums[i];
            nums[i] = nums[index];
            nums[index] = tmp;
        }

        for (int i = 0; i < 9; i++) {
            sb.append(nums[i]);
        }

        // 맨앞에 유저 아이디에 10을 나눈 나머지를 넣어준다.
        return (id % 10) + sb.toString();
    }

    // 보유 계좌 갯수 체크
    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
        }
    }

    @Transactional
    public Account getAccount(Long id) {
        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() > 0) {
            throw new AccountException(ErrorCode.BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }

    private AccountUser getAccountUser(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        return accountUser;
    }

}

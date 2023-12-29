package com.example.account.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue
    private Long id;

    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

}
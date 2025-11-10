/*
 * @(#)OAuthCredentialEntity.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "oauth_credential")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuthCredentialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    private UUID oauthCredentialId;
    
    @ManyToOne
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccountEntity userAccount;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerUserId; // ID retornado pela plataforma de terceiro.

    @Column(nullable = true)
    private String refreshToken;
}

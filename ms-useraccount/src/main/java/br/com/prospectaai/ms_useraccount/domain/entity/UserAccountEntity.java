/*
 * @(#)UserAccountEntity.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.prospectaai.ms_useraccount.domain.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_account")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    private UUID accountId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 50)
    private String displayName;

    @Column(nullable = true)
    private String avatarUrl;
    
    @Column(nullable = false)
    private boolean active = true;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.SimpleUser;

    @OneToOne(mappedBy = "userAccount", cascade = CascadeType.ALL)
    private LocalCredentialEntity LocalCredential;
    
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL)
    private List<OAuthCredentialEntity> oatuhCredentials = new ArrayList<>();
}

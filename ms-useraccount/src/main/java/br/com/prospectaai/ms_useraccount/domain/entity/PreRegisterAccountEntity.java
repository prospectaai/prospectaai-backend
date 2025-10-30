/*
 * @(#)PreRegisterAccountEntity.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.entity;

import java.util.UUID;

import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pre_register_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreRegisterAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID preRegisterId;

    @Column(nullable = false, length = 50)
    private String displayName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PreRegisterScope scope;
    
    private String passwordHash;
}

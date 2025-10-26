package br.com.prospectaai.ms_useraccount.entity;

import java.util.UUID;

import br.com.prospectaai.ms_useraccount.enums.OAuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuthCredentialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false)
    private UUID oauthCredentialId;
    
    @ManyToOne
    private UserAccountEntity userAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    @Column(nullable = false)
    private String providerUserId; // ID retornado pela plataforma de terceiro.

    @Column(nullable = true)
    private String acceessToken;

    @Column(nullable = true)
    private String refreshToken;
}

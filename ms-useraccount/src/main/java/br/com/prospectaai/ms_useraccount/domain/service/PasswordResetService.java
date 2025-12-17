package br.com.prospectaai.ms_useraccount.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.prospectaai.ms_useraccount.domain.entity.LocalCredentialEntity;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.repository.LocalCredentialRepository;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserAccountRepository userAccountRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    public boolean initiate(String email) {
        Optional<UserAccountEntity> userOpt = userAccountRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;

        UserAccountEntity user = userOpt.get();
        if (user.getLocalCredential() == null) return false; // ignora contas OAuth

        UUID resetId = UUID.randomUUID();
        String token = jwtTokenProvider.generateResetPasswordToken(user.getEmail(), resetId);

        LocalCredentialEntity cred = user.getLocalCredential();
        cred.setResetPasswordToken(token);
        localCredentialRepository.save(cred);

        String link = frontendBaseUrl + "/auth/reset-password?token=" + token;
        emailService.sendResetPasswordEmail(user.getEmail(), user.getDisplayName(), link);
        return true;
    }

    public boolean validateToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) return false;
        if (!jwtTokenProvider.isResetPasswordToken(token)) return false;
        String email = jwtTokenProvider.extractUsername(token);
        Optional<UserAccountEntity> userOpt = userAccountRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;
        UserAccountEntity user = userOpt.get();
        if (user.getLocalCredential() == null) return false;
        String stored = user.getLocalCredential().getResetPasswordToken();
        return stored != null && stored.equals(token);
    }

    public boolean resetPassword(String token, String newPassword) {
        if (!validateToken(token)) return false;
        String email = jwtTokenProvider.extractUsername(token);
        UserAccountEntity user = userAccountRepository.findByEmail(email).orElse(null);
        if (user == null || user.getLocalCredential() == null) return false;
        LocalCredentialEntity cred = user.getLocalCredential();
        cred.setPasswordHash(passwordEncoder.encode(newPassword));
        cred.setResetPasswordToken(null);
        localCredentialRepository.save(cred);
        return true;
    }

    // envio de email delegado para EmailService
}
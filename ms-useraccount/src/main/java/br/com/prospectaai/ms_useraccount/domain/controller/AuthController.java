/*
 * @(#)AuthController.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.domain.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import br.com.prospectaai.ms_useraccount.domain.dto.LoginRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.LoginResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.service.BillingClient;
import br.com.prospectaai.shared.dto.billing.SubscriptionStatusResponse;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userRepository;
    private final BillingClient billingClient;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println("pronto para fazer o login");
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())

        );
        System.out.println("Login realizado com sucesso");

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserAccountEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        System.out.println("Usuario logado: " + user.getEmail());

        String token = jwtTokenProvider.generateToken(user);
        System.out.println("Token: " + token);
        System.out.println("Email: " + user.getEmail());
        System.out.println("DisplayName: " + user.getDisplayName());
        return ResponseEntity.ok(new LoginResponse(
            token,
            LocalDateTime.ofInstant(jwtTokenProvider.extractExpiration(token).toInstant(), ZoneId.systemDefault())
        ));
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validate(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid");
            }
            
            String token = authorizationHeader.substring("Bearer ".length());
            boolean valid = jwtTokenProvider.validateToken(token) && userRepository.existsByEmail(jwtTokenProvider.extractUsername(token));
            
            if (valid) {
                String email = jwtTokenProvider.extractUsername(token);
                UserAccountEntity user = userRepository.findByEmail(email).orElse(null);
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid");
                }
                SubscriptionStatusResponse sub = billingClient.getSubscriptionStatus(user.getAccountId());
                boolean subActive = sub != null && sub.isActive();
                if (!subActive) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid");
                }
                return ResponseEntity.ok(email);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid");
        }
    }
}

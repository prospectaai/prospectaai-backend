package br.com.prospectaai.ms_useraccount.domain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.prospectaai.ms_useraccount.domain.dto.LoginRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.LoginResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserAccountEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = jwtTokenProvider.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token, user.getDisplayName(), user.getEmail()));
    }
}

/*
 * @(#)OAuth2SuccessHandler.java
 *
 * Copyright 2025, Prospecta AI
 * https://www.prospectaai.com.br
 *
 * Todos os direitos reservados.
 */

package br.com.prospectaai.ms_useraccount.oauth;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import br.com.prospectaai.ms_useraccount.domain.dto.RegisterRequest;
import br.com.prospectaai.ms_useraccount.domain.dto.RegisterResponse;
import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.enums.PreRegisterScope;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import br.com.prospectaai.ms_useraccount.domain.service.PreRegisterAccountService;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserAccountRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PreRegisterAccountService preRegisterAccountService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // procura user por e-mail ou cria
        UserAccountEntity user = userRepository.findByEmail(email).orElseGet(() -> null );
        boolean isNeedPreRegister = user == null;
        RegisterResponse res = isNeedPreRegister ? preRegisterAccountService.doPreRegister(
                RegisterRequest.builder()
                    .displayName(oAuth2User.getAttribute("name"))
                    .email(email)
                    .avatarUrl(oAuth2User.getAttribute("picture"))
                    .scope(PreRegisterScope.OAUTH2)
                    .build()
            ) : null;

        // Gera JWT e redireciona com token
        String jwt = isNeedPreRegister ?  jwtTokenProvider.generateToken(res) : jwtTokenProvider.generateToken(user);
        response.sendRedirect("/oauth2/success?token=" + jwt);
    }
}

package br.com.prospectaai.ms_useraccount.oauth;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import br.com.prospectaai.ms_useraccount.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserAccountRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // procura user por e-mail ou cria
        UserAccountEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            UserAccountEntity newUser = new UserAccountEntity();
            newUser.setEmail(email);
            newUser.setDisplayName(oAuth2User.getAttribute("name"));
            newUser.setAvatarUrl(oAuth2User.getAttribute("picture"));
            return userRepository.save(newUser);
        });

        // Gera JWT e redireciona com token
        String jwt = jwtTokenProvider.generateToken(user);
        response.sendRedirect("/oauth2/success?token=" + jwt);
    }
}

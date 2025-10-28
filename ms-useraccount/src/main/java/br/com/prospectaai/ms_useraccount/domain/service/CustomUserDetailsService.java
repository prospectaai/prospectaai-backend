package br.com.prospectaai.ms_useraccount.domain.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.prospectaai.ms_useraccount.domain.entity.UserAccountEntity;
import br.com.prospectaai.ms_useraccount.domain.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccountEntity user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        if (user.getLocalCredential() == null) {
            throw new UsernameNotFoundException("Usuário não possui credenciais locais");
        }

        return User.builder()
                .username(user.getEmail())
                .password(user.getLocalCredential().getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}

package br.com.prospectaai.ms_useraccount.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.prospectaai.ms_useraccount.domain.entity.OAuthCredentialEntity;

public interface OAuthCredentialRepository extends JpaRepository<OAuthCredentialEntity, UUID> {

}

package com.mybank.auth.repository;

import com.mybank.auth.model.AuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for AuthCredential entity
 */
@Repository
public interface AuthCredentialRepository extends JpaRepository<AuthCredential, String> {

    Optional<AuthCredential> findByEmail(String email);

    Optional<AuthCredential> findByUserId(String userId);

    boolean existsByEmail(String email);

    boolean existsByUserId(String userId);
}

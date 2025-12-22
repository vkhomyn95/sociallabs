package com.workflow.sociallabs.domain.repository;

import com.workflow.sociallabs.domain.entity.Credential;
import com.workflow.sociallabs.domain.enums.CredentialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository для роботи з Credential
 */
@Repository
public interface CredentialRepository extends JpaRepository<Credential, Long> {

    /**
     * Знайти credentials за типом
     */
    List<Credential> findByType(CredentialType type);

    /**
     * Знайти credential за назвою
     */
    Optional<Credential> findByName(String name);

    /**
     * Пошук credentials за назвою (like)
     */
    List<Credential> findByNameContainingIgnoreCase(String name);

    /**
     * Знайти credentials за типом та назвою
     */
    List<Credential> findByTypeAndNameContainingIgnoreCase(
            CredentialType type,
            String name
    );

    /**
     * Перевірити чи використовується credential
     */
    @Query("""
        SELECT CASE WHEN COUNT(ni) > 0 THEN true ELSE false END
        FROM Node ni
        WHERE ni.credential.id = :credentialId
        """)
    boolean isCredentialInUse(@Param("credentialId") Long credentialId);

    /**
     * Знайти всі credentials створені після дати
     */
    List<Credential> findByCreatedAtAfter(LocalDateTime date);
}

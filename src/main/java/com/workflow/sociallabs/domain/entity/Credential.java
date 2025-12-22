package com.workflow.sociallabs.domain.entity;

import com.workflow.sociallabs.domain.enums.CredentialType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "credentials", indexes = {
        @Index(name = "idx_credential_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated
    @Column(nullable = false, length = 50)
    private CredentialType type; // TELEGRAM, HTTP_AUTH, API_KEY, etc.

    @Column(name = "encrypted_data", columnDefinition = "TEXT", nullable = false)
    private String encryptedData; // Encrypted JSON with credentials

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

package com.mybank.common.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * User Session Data Model
 *
 * Stored in Redis for global session management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    private String userId;

    /**
     * Username/Email
     */
    private String username;

    /**
     * User roles
     */
    private Set<String> roles;

    /**
     * JWT token
     */
    private String token;

    /**
     * Session creation time
     */
    private LocalDateTime createdAt;

    /**
     * Last access time
     */
    private LocalDateTime lastAccessedAt;

    /**
     * Client IP address
     */
    private String ipAddress;

    /**
     * User agent
     */
    private String userAgent;

    /**
     * Additional session attributes
     */
    private java.util.Map<String, Object> attributes;
}

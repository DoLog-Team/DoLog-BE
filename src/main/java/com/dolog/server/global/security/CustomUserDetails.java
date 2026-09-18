package com.dolog.server.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final long sessionId;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    private final boolean accountNonExpired = true;
    private final boolean accountNonLocked = true;
    private final boolean credentialsNonExpired = true;
    private final boolean enabled = true;

    public CustomUserDetails(UUID id,
                             long sessionId,
                             String password,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.sessionId = sessionId;
        this.username = id.toString();
        this.password = password;
        this.authorities = authorities;
    }
}

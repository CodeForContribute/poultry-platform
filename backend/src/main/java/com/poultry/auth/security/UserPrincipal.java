package com.poultry.auth.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private UUID id;
    private String userType; // SELLER_USER, BUYER, ADMIN
    private String email;
    private String role;
    private UUID sellerId; // Only for seller users
    private boolean enabled;
    private boolean mustChangePassword;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + userType),
                new SimpleGrantedAuthority("ROLE_" + role)
        );
    }

    @Override
    public String getPassword() {
        return null; // Not needed for JWT auth
    }

    @Override
    public String getUsername() {
        return email != null ? email : id.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !mustChangePassword;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSeller() {
        return "SELLER_USER".equals(userType);
    }

    public boolean isBuyer() {
        return "BUYER".equals(userType);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(userType);
    }

    public boolean isSellerAdmin() {
        return isSeller() && "SELLER_ADMIN".equals(role);
    }
}

package org.example.authservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.example.authservice.model.enums.UserRoleType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(unique = true)
    private String phoneNumber;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    private String address;
    
    private LocalDateTime birthDate;

    @Column(name = "security_question")
    private String securityQuestion;
    
    @Column(name = "security_answer_hash")
    private String securityAnswerHash;
    
    @Column(name = "security_question_enabled")
    @Builder.Default
    private boolean securityQuestionEnabled = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRoleType userRoleType;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    @Builder.Default
    private boolean enabled = false;
    
    @Builder.Default
    private boolean locked = false;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;

    @Column(name = "agency_id")
    private String agencyId;  //  de l'agence (référence vers Account Service)
    
    @Column(name = "agency_code")
    private String agencyCode;  //  de l'agence (pour recherche rapide)
    
    @Column(name = "assigned_by")
    private String assignedBy;  //  a assigné l'agent à l'agence
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;  //  d'assignation
    
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    
    @Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> authorities = new HashSet<>();
    
    //  les permissions existantes
    roles.stream()
        .flatMap(role -> role.getPermissions().stream())
        .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.getName())));
    
    
    roles.stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .forEach(authorities::add);
    
    return authorities;
}
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
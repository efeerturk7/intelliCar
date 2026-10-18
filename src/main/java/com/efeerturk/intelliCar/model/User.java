package com.efeerturk.intelliCar.model;

import com.efeerturk.intelliCar.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String phone;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    // DÜZELTME: eskiden "" dönüyordu; JWT doğrulama ve login her zaman başarısız olurdu.
    @Override
    public String getPassword() {
        return passwordHash;
    }

    // DÜZELTME: kullanıcı adı olarak e-posta kullanılıyor (JwtService subject = email).
    @Override
    public String getUsername() {
        return email;
    }
}

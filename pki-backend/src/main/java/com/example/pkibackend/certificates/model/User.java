package com.example.pkibackend.certificates.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "USER_")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String keycloakId;

    @Column(unique = true)
    private String email;

    @Column
    private String firstname;

    @Column
    private String lastname;

    @Column
    private String organization;

    @Column
    private String department;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_certificates",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "certificate_serial")
    )
    private Set<Certificate> certificates = new HashSet<>();
}
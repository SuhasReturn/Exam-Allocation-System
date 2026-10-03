package com.suhas.examallocation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Stores login credentials and links them to a role.
 *
 * For STUDENT and FACULTY roles, the account points to the
 * corresponding student or faculty record via optional foreign keys.
 * An ADMIN account has both set to null.
 *
 * password_hash stores the BCrypt-hashed password, never plaintext.
 */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_student_id")
    private Student linkedStudent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_faculty_id")
    private Faculty linkedFaculty;

    public UserAccount() {
    }

    public UserAccount(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Student getLinkedStudent() {
        return linkedStudent;
    }

    public void setLinkedStudent(Student linkedStudent) {
        this.linkedStudent = linkedStudent;
    }

    public Faculty getLinkedFaculty() {
        return linkedFaculty;
    }

    public void setLinkedFaculty(Faculty linkedFaculty) {
        this.linkedFaculty = linkedFaculty;
    }
}

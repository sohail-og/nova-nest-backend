package com.novanest.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String otp;

    @Column(name = "expiry_time")
    private LocalDateTime expiresAt;

    @Column(columnDefinition = "boolean default false")
    private boolean verified;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "integer default 0")
    private int attempts = 0;

    @Column(name = "resend_attempts", columnDefinition = "integer default 0")
    private int resendAttempts = 0;

    public OtpToken() {
    }

    public OtpToken(User user, String otp, LocalDateTime expiresAt) {
        this.user = user;
        this.email = user.getEmail(); // Maintain synchronization with the existing DB NOT NULL column
        this.otp = otp;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.attempts = 0;
        this.resendAttempts = 0;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getResendAttempts() {
        return resendAttempts;
    }

    public void setResendAttempts(int resendAttempts) {
        this.resendAttempts = resendAttempts;
    }
}

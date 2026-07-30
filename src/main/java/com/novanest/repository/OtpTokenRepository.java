package com.novanest.repository;

import com.novanest.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Integer> {

    Optional<OtpToken> findByEmail(String email);

    Optional<OtpToken> findByEmailAndOtp(String email, String otp);

    Optional<OtpToken> findByEmailAndVerifiedTrue(String email);

    void deleteByEmail(String email);
}

package com.novanest.repository;

import com.novanest.model.OtpToken;
import com.novanest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Integer> {

    Optional<OtpToken> findByUser(User user);

    Optional<OtpToken> findByUserAndOtp(User user, String otp);

    Optional<OtpToken> findByUserAndVerifiedTrue(User user);

    void deleteByUser(User user);
}

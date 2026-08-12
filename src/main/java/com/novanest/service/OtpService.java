package com.novanest.service;

import com.novanest.model.OtpToken;
import com.novanest.model.User;
import com.novanest.repository.OtpTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.security.SecureRandom;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public OtpService(OtpTokenRepository otpTokenRepository, PasswordEncoder passwordEncoder) {
        this.otpTokenRepository = otpTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String generateOtp(User user) {
        otpTokenRepository.deleteByUser(user);

        SecureRandom random = new SecureRandom();
        String plainOtp = String.format("%06d", random.nextInt(1000000));
        String hashedOtp = passwordEncoder.encode(plainOtp);

        OtpToken otpToken = new OtpToken(user, hashedOtp, LocalDateTime.now().plusMinutes(5));
        otpTokenRepository.save(otpToken);
        
        logger.info("OTP generated for user {}", user.getEmail());
        return plainOtp;
    }

    @Transactional
    public boolean verifyOtp(User user, String otp) {
        Optional<OtpToken> tokenOpt = otpTokenRepository.findByUser(user);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        OtpToken otpToken = tokenOpt.get();

        if (otpToken.getAttempts() >= 3) {
            logger.warn("Maximum OTP attempts reached for user {}", user.getEmail());
            otpTokenRepository.delete(otpToken);
            return false;
        }

        otpToken.setAttempts(otpToken.getAttempts() + 1);

        if (LocalDateTime.now().isAfter(otpToken.getExpiresAt())) {
            logger.info("OTP expired for user {}", user.getEmail());
            otpTokenRepository.delete(otpToken);
            return false;
        }

        if (!passwordEncoder.matches(otp, otpToken.getOtp())) {
            logger.info("Invalid OTP attempt for user {}", user.getEmail());
            otpTokenRepository.save(otpToken);
            return false;
        }

        otpToken.setVerified(true);
        otpTokenRepository.save(otpToken);
        logger.info("OTP verified for user {}", user.getEmail());
        return true;
    }

    public boolean isEmailOtpVerified(User user) {
        return otpTokenRepository.findByUserAndVerifiedTrue(user)
                .map(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public void clearOtp(User user) {
        otpTokenRepository.deleteByUser(user);
    }
    
    public Optional<OtpToken> getOtpToken(User user) {
        return otpTokenRepository.findByUser(user);
    }
    
    @Transactional
    public void saveOtpToken(OtpToken token) {
        otpTokenRepository.save(token);
    }
}
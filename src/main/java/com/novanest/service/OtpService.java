package com.novanest.service;

import com.novanest.model.OtpToken;
import com.novanest.repository.OtpTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;

    public OtpService(OtpTokenRepository otpTokenRepository) {
        this.otpTokenRepository = otpTokenRepository;
    }

    @Transactional
    public String generateOtp(String email) {
        // Delete any existing OTP for this email first
        otpTokenRepository.deleteByEmail(email);

        Random random = new Random();
        String otp = String.format("%06d", random.nextInt(1000000));

        OtpToken otpToken = new OtpToken(email, otp, LocalDateTime.now().plusMinutes(5));
        otpTokenRepository.save(otpToken);

        return otp;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {
        Optional<OtpToken> tokenOpt = otpTokenRepository.findByEmailAndOtp(email, otp);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        OtpToken otpToken = tokenOpt.get();

        if (LocalDateTime.now().isAfter(otpToken.getExpiryTime())) {
            otpTokenRepository.delete(otpToken);
            return false;
        }

        // OTP is correct and not expired. Mark it as verified for reset-password endpoint validation
        otpToken.setVerified(true);
        otpTokenRepository.save(otpToken);
        return true;
    }

    public boolean isEmailOtpVerified(String email) {
        return otpTokenRepository.findByEmailAndVerifiedTrue(email)
                .map(token -> token.getExpiryTime().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public void clearOtp(String email) {
        otpTokenRepository.deleteByEmail(email);
    }
}
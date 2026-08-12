package com.novanest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("Nova Nest Password Reset OTP");

        message.setText(
                "Dear User,\n\n"
                        + "Your OTP for password reset is: "
                        + otp
                        + "\n\nThis OTP is valid for 5 minutes."
                        + "\n\nRegards,\nNova Nest Team");

        mailSender.send(message);
    }

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    public void sendResetLink(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Nova Nest Password Reset Link");

        String resetLink = frontendUrl + "/reset-password?token=" + token + "&email=" + toEmail;

        message.setText(
                "Dear User,\n\n"
                        + "Please click the following link to reset your password:\n"
                        + resetLink
                        + "\n\nThis link is valid for 1 hour."
                        + "\n\nRegards,\nNova Nest Team");

        mailSender.send(message);
    }


}
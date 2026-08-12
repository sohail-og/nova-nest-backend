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
}

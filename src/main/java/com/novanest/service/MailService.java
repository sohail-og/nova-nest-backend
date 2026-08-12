package com.novanest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class MailService {

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${MAIL_FROM:}")
    private String mailFrom;

    @org.springframework.scheduling.annotation.Async
    public void sendOtp(String toEmail, String otp) {
        if (resendApiKey == null || resendApiKey.isEmpty() || mailFrom == null || mailFrom.isEmpty()) {
            System.err.println("RESEND_API_KEY or MAIL_FROM is not configured. Email not sent.");
            return;
        }

        try {
            String subject = "Nova Nest Password Reset OTP";
            String htmlContent = "<p>Dear User,</p><p>Your Nova Nest password reset OTP is: <strong>" + otp + "</strong></p><p>This OTP is valid for 5 minutes.</p><p>Regards,<br/>Nova Nest Team</p>";

            // Escape JSON manually to avoid depending on specific JSON libraries if possible, or use a simple string replace
            String jsonPayload = String.format(
                    "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                    mailFrom, toEmail, subject, htmlContent.replace("\"", "\\\"")
            );

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("OTP email sent successfully to " + toEmail);
            } else {
                System.err.println("Failed to send OTP email via Resend API. Status code: " + response.statusCode());
                System.err.println("Response body: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Exception while sending OTP email to " + toEmail + ": " + e.getMessage());
        }
    }
}
package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.model.User;
import com.samarama.bicycle.api.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = Logger.getLogger(EmailServiceImpl.class.getName());

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void sendVerificationEmail(User user, String token) {
        try {
            String verificationUrl = frontendUrl + "/verify-account?token=" + token;

            String subject = "Potwierdź swoje konto w aplikacji Samarama Bicycle";
            String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #3498db;'>Witaj " + user.getFirstName() + "!</h2>"
                    + "<p>Dziękujemy za rejestrację w aplikacji Samarama Bicycle.</p>"
                    + "<p>Aby aktywować swoje konto, kliknij poniższy link:</p>"
                    + "<p><a href='" + verificationUrl + "' style='display: inline-block; padding: 10px 20px; background-color: #3498db; color: #ffffff; text-decoration: none; border-radius: 5px;'>Potwierdź konto</a></p>"
                    + "<p>Link aktywacyjny jest ważny przez 24 godziny.</p>"
                    + "<p>Jeśli to nie Ty zakładałeś konto, zignoruj tę wiadomość.</p>"
                    + "<p>Pozdrawiamy,<br>Zespół Samarama Bicycle</p>"
                    + "</div>";

            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Verification email sent to: " + user.getEmail());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending verification email to " + user.getEmail(), e);
        }
    }

    @Async
    @Override
    public void sendAccountActivatedEmail(User user) {
        try {
            String loginUrl = frontendUrl + "/login";

            String subject = "Twoje konto w aplikacji Samarama Bicycle zostało aktywowane";
            String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #3498db;'>Witaj " + user.getFirstName() + "!</h2>"
                    + "<p>Twoje konto w aplikacji Samarama Bicycle zostało pomyślnie aktywowane.</p>"
                    + "<p>Możesz teraz w pełni korzystać z naszej aplikacji.</p>"
                    + "<p><a href='" + loginUrl + "' style='display: inline-block; padding: 10px 20px; background-color: #3498db; color: #ffffff; text-decoration: none; border-radius: 5px;'>Zaloguj się</a></p>"
                    + "<p>Pozdrawiamy,<br>Zespół Samarama Bicycle</p>"
                    + "</div>";

            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Account activated email sent to: " + user.getEmail());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending account activated email to " + user.getEmail(), e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
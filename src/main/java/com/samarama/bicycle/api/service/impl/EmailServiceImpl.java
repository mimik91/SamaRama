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
import jakarta.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;
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

            // Lepszy tytuł maila - bez słów kluczowych spamowych
            String subject = "Dokończ rejestrację w aplikacji Samarama Bicycle";

            // Ulepszona treść maila
            String content =
                    "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>" +
                            "<div style='text-align: center; margin-bottom: 20px;'>" +
                            "<h1 style='color: #3498db; margin-bottom: 5px;'>Samarama Bicycle</h1>" +
                            "<p style='color: #7f8c8d; font-size: 16px;'>Twoja aplikacja do zarządzania rowerami</p>" +
                            "</div>" +
                            "<p style='font-size: 16px; color: #333; margin-bottom: 20px;'>Cześć " + user.getFirstName() + ",</p>" +
                            "<p style='font-size: 16px; color: #333; margin-bottom: 20px;'>Dziękujemy za rejestrację w aplikacji Samarama Bicycle. Aby dokończyć proces rejestracji, prosimy o potwierdzenie swojego adresu email.</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='" + verificationUrl + "' style='display: inline-block; padding: 12px 24px; background-color: #3498db; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;'>Potwierdź swój email</a>" +
                            "</div>" +
                            "<p style='font-size: 14px; color: #7f8c8d; margin-bottom: 10px;'>Jeśli przycisk nie działa, możesz skopiować i wkleić poniższy link do przeglądarki:</p>" +
                            "<p style='font-size: 14px; color: #3498db; word-break: break-all; margin-bottom: 30px;'>" + verificationUrl + "</p>" +
                            "<p style='font-size: 14px; color: #7f8c8d; margin-bottom: 10px;'>Jeżeli nie rejestrowałeś się w naszej aplikacji, zignoruj tę wiadomość.</p>" +
                            "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
                            "<p style='font-size: 12px; color: #7f8c8d; text-align: center;'>© " + java.time.Year.now().getValue() + " Samarama Bicycle. Wszelkie prawa zastrzeżone.</p>" +
                            "</div>";

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

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Ustawienie nagłówków pomagających uniknąć oznaczenia jako spam
        helper.setFrom(new InternetAddress(fromEmail, "Samarama Bicycle"));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // Dodanie dodatkowych nagłówków
        message.addHeader("X-Priority", "1");
        message.addHeader("X-MSMail-Priority", "High");
        message.addHeader("Importance", "High");
        message.addHeader("X-Mailer", "Samarama Bicycle Application");
        message.addHeader("List-Unsubscribe", "<mailto:" + fromEmail + "?subject=Unsubscribe>");

        mailSender.send(message);
    }
}
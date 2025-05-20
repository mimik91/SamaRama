package com.samarama.bicycle.api.service.impl;

import com.samarama.bicycle.api.dto.ServiceRegisterDto;
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

    @Value("${admin.notification.email}")
    private String adminEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void sendVerificationEmail(User user, String token) {
        try {
            String verificationUrl = frontendUrl + "/verify-account?token=" + token;

            // Lepszy tytuł maila - bez słów kluczowych spamowych
            String subject = "Dokończ rejestrację w aplikacji cyclopick.pl";

            // Ulepszona treść maila
            String content =
                    "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>" +
                            "<div style='text-align: center; margin-bottom: 20px;'>" +
                            "<h1 style='color: #3498db; margin-bottom: 5px;'>cyclopick.pl</h1>" +
                            "<p style='color: #7f8c8d; font-size: 16px;'>Twoja aplikacja do zarządzania rowerami</p>" +
                            "</div>" +
                            "<p style='font-size: 16px; color: #333; margin-bottom: 20px;'>Cześć " + user.getFirstName() + ",</p>" +
                            "<p style='font-size: 16px; color: #333; margin-bottom: 20px;'>Dziękujemy za rejestrację w aplikacji cyclopick.pl. Aby dokończyć proces rejestracji, prosimy o potwierdzenie swojego adresu email.</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='" + verificationUrl + "' style='display: inline-block; padding: 12px 24px; background-color: #3498db; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;'>Potwierdź swój email</a>" +
                            "</div>" +
                            "<p style='font-size: 14px; color: #7f8c8d; margin-bottom: 10px;'>Jeśli przycisk nie działa, możesz skopiować i wkleić poniższy link do przeglądarki:</p>" +
                            "<p style='font-size: 14px; color: #3498db; word-break: break-all; margin-bottom: 30px;'>" + verificationUrl + "</p>" +
                            "<p style='font-size: 14px; color: #7f8c8d; margin-bottom: 10px;'>Jeżeli nie rejestrowałeś się w naszej aplikacji, zignoruj tę wiadomość.</p>" +
                            "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
                            "<p style='font-size: 12px; color: #7f8c8d; text-align: center;'>© " + java.time.Year.now().getValue() + " cyclopick.pl. Wszelkie prawa zastrzeżone.</p>" +
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

            String subject = "Twoje konto w aplikacji cyclopick.pl zostało aktywowane";
            String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #3498db;'>Witaj " + user.getFirstName() + "!</h2>"
                    + "<p>Twoje konto w aplikacji cyclopick.pl zostało pomyślnie aktywowane.</p>"
                    + "<p>Możesz teraz w pełni korzystać z naszej aplikacji.</p>"
                    + "<p><a href='" + loginUrl + "' style='display: inline-block; padding: 10px 20px; background-color: #3498db; color: #ffffff; text-decoration: none; border-radius: 5px;'>Zaloguj się</a></p>"
                    + "<p>Pozdrawiamy,<br>Zespół cyclopick.pl</p>"
                    + "</div>";

            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Account activated email sent to: " + user.getEmail());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending account activated email to " + user.getEmail(), e);
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(User user, String token) {
        try {
            String resetUrl = frontendUrl + "/password-reset?token=" + token;

            String subject = "Resetowanie hasła w aplikacji cyclopick.pl";

            String content =
                    "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>" +
                            "<div style='text-align: center; margin-bottom: 20px;'>" +
                            "<h1 style='color: #3498db; margin-bottom: 5px;'>cyclopick.pl</h1>" +
                            "<p style='color: #7f8c8d; font-size: 16px;'>Twoja aplikacja do zarządzania rowerami</p>" +
                            "</div>" +
                            "<p style='font-size: 16px; color: #333; margin-bottom: 20px;'>Cześć " + user.getFirstName() + ",</p>" +
                            "<p style='font-size: 16px; color: #333; margin-bottom: 20px;'>Otrzymaliśmy prośbę o zresetowanie hasła do Twojego konta. Aby ustawić nowe hasło, kliknij w poniższy przycisk:</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='" + resetUrl + "' style='display: inline-block; padding: 12px 24px; background-color: #3498db; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;'>Resetuj hasło</a>" +
                            "</div>" +
                            "<p style='font-size: 14px; color: #7f8c8d; margin-bottom: 10px;'>Link jest ważny przez 2 godziny. Jeśli przycisk nie działa, możesz skopiować i wkleić poniższy link do przeglądarki:</p>" +
                            "<p style='font-size: 14px; color: #3498db; word-break: break-all; margin-bottom: 30px;'>" + resetUrl + "</p>" +
                            "<p style='font-size: 14px; color: #7f8c8d; margin-bottom: 10px;'>Jeżeli nie prosiłeś o zresetowanie hasła, zignoruj tę wiadomość lub skontaktuj się z nami.</p>" +
                            "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
                            "<p style='font-size: 12px; color: #7f8c8d; text-align: center;'>© " + java.time.Year.now().getValue() + " cyclopick.pl. Wszelkie prawa zastrzeżone.</p>" +
                            "</div>";

            sendHtmlEmail(user.getEmail(), subject, content);
            logger.info("Password reset email sent to: " + user.getEmail());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending password reset email to " + user.getEmail(), e);
            throw new RuntimeException("Error sending password reset email", e);
        }
    }

    @Async
    @Override
    public void sendServiceRegistrationNotification(ServiceRegisterDto dto) {
        try {
            String subject = "Nowe zgłoszenie rejestracji serwisu: " + dto.getServiceName();

            StringBuilder content = new StringBuilder();
            content.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
            content.append("<h2>Nowe zgłoszenie rejestracji serwisu</h2>");
            content.append("<p><strong>Nazwa serwisu:</strong> ").append(dto.getServiceName()).append("</p>");
            content.append("<p><strong>Osoba kontaktowa:</strong> ").append(dto.getName()).append("</p>");
            content.append("<p><strong>Telefon:</strong> ").append(dto.getPhoneNumber()).append("</p>");
            content.append("<p><strong>Email:</strong> ").append(dto.getEmail()).append("</p>");
            content.append("<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>");
            content.append("<p style='font-size: 12px; color: #7f8c8d; text-align: center;'>© ").append(java.time.Year.now().getValue()).append(" cyclopick.pl</p>");
            content.append("</div>");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, "cyclopick.pl - Administracja"));
            helper.setTo(adminEmail);
            helper.setSubject(subject);
            helper.setText(content.toString(), true);

            // Dodanie nagłówków
            message.addHeader("X-Priority", "1");
            message.addHeader("X-MSMail-Priority", "High");
            message.addHeader("Importance", "High");
            message.addHeader("X-Mailer", "cyclopick.pl Administration");

            mailSender.send(message);
            logger.info("Service registration notification sent to admin email: " + adminEmail);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending service registration notification: " + e.getMessage(), e);
            throw new RuntimeException("Error sending service registration notification", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Ustawienie nagłówków pomagających uniknąć oznaczenia jako spam
        helper.setFrom(new InternetAddress(fromEmail, "cyclopick.pl"));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // Dodanie dodatkowych nagłówków
        message.addHeader("X-Priority", "1");
        message.addHeader("X-MSMail-Priority", "High");
        message.addHeader("Importance", "High");
        message.addHeader("X-Mailer", "cyclopick.pl Application");
        message.addHeader("List-Unsubscribe", "<mailto:" + fromEmail + "?subject=Unsubscribe>");

        mailSender.send(message);
    }
}
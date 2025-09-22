package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.ServiceRegisterDto;
import com.samarama.bicycle.api.dto.TransportOrderDto;
import com.samarama.bicycle.api.model.IndividualUser;
import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.ServiceUser;
import com.samarama.bicycle.api.model.User;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface EmailService {
    /**
     * Wysyła mail z linkiem aktywacyjnym
     * @param user użytkownik do którego wysyłamy mail
     * @param token token weryfikacyjny
     */
    void sendVerificationEmail(User user, String token);

    /**
     * Wysyła mail potwierdzający aktywację konta
     * @param user użytkownik którego konto zostało aktywowane
     */
    void sendAccountActivatedEmail(User user);

    void sendPasswordResetEmail(User user, String token);
    void sendServiceRegistrationNotification(ServiceRegisterDto serviceRegisterDto);
    void sendOrderNotificationEmail(ServiceOrder serviceOrder);
    void sendTransportOrderNotificationEmail(String recipientEmail, List<TransportOrderDto> orders);

}
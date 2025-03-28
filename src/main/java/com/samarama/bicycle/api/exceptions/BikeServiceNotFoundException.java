package com.samarama.bicycle.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BikeServiceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BikeServiceNotFoundException(String message) {
        super(message);
    }

    public BikeServiceNotFoundException(Long id) {
        super(String.format("Bike service with ID %d not found", id));
    }

    public BikeServiceNotFoundException(String email, String field) {
        super(String.format("Bike service with %s '%s' not found", field, email));
    }

    public static BikeServiceNotFoundException withId(Long id) {
        return new BikeServiceNotFoundException(id);
    }

    public static BikeServiceNotFoundException withEmail(String email) {
        return new BikeServiceNotFoundException(email, "email");
    }

    public static BikeServiceNotFoundException withCustomMessage(String message) {
        return new BikeServiceNotFoundException(message);
    }
}

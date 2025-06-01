package com.samarama.bicycle.api.dto;

import com.samarama.bicycle.api.model.ServiceOrder;
import com.samarama.bicycle.api.model.TransportOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO dla unified view zamówień serwisowych i transportowych
 * Umożliwia wyświetlanie obu typów zamówień w jednej tabeli/liście
 */
public record ServiceAndTransportOrdersDto(
        Long id,
        String orderType, // "SERVICE" lub "TRANSPORT"

        // === INFORMACJE O ROWERZE ===
        Long bicycleId,
        String bicycleBrand,
        String bicycleModel,
        String bicycleType,

        // === INFORMACJE O KLIENCIE ===
        String clientEmail,
        String clientPhone,
        String clientName, // firstName + lastName dla User, null dla IncompleteUser

        // === INFORMACJE O ODBIORZE ===
        LocalDate pickupDate,
        String pickupAddress,
        Double pickupLatitude,
        Double pickupLongitude,
        LocalTime pickupTimeFrom,
        LocalTime pickupTimeTo,

        // === INFORMACJE O DOSTAWIE ===
        String deliveryAddress, // "SERWIS WŁASNY" dla zamówień serwisowych, rzeczywisty adres dla transportu
        Double deliveryLatitude,
        Double deliveryLongitude,
        String targetServiceName,

        // === CENY ===
        BigDecimal transportPrice,
        BigDecimal servicePrice, // null dla czystego transportu
        BigDecimal totalPrice,

        // === DATY I STATUS ===
        LocalDateTime orderDate,
        String status,
        LocalDateTime actualPickupTime,
        LocalDateTime actualDeliveryTime,

        // === INFORMACJE O SERWISIE (tylko dla ServiceOrder) ===
        String servicePackageCode,
        String servicePackageName,
        LocalDateTime serviceStartDate,
        LocalDateTime serviceCompletionDate,
        String serviceNotes,

        // === NOTATKI ===
        String transportNotes,
        String additionalNotes,

        // === METADANE ===
        String lastModifiedBy,
        LocalDateTime lastModifiedDate,

        // === DODATKOWE INFORMACJE ===
        Integer estimatedTime, // w minutach
        String serviceDurationDisplay // dla ServiceOrder - czas trwania serwisu
) {
    /**
     * Tworzy DTO z zamówienia serwisowego
     */
    public static ServiceAndTransportOrdersDto fromServiceOrder(ServiceOrder entity) {
        String clientName = extractClientName(entity.getClient());

        return new ServiceAndTransportOrdersDto(
                entity.getId(),
                "SERVICE",

                // Rower
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getBicycle() != null ? entity.getBicycle().getBrand() : null,
                entity.getBicycle() != null ? entity.getBicycle().getModel() : null,
                entity.getBicycle() != null ? entity.getBicycle().getType() : null,

                // Klient
                entity.getClient() != null ? entity.getClient().getEmail() : null,
                entity.getClient() != null ? entity.getClient().getPhoneNumber() : null,
                clientName,

                // Odbiór
                entity.getPickupDate(),
                entity.getPickupAddress(),
                entity.getPickupLatitude(),
                entity.getPickupLongitude(),
                entity.getPickupTimeFrom(),
                entity.getPickupTimeTo(),

                // Dostawa
                entity.getDeliveryAddress(),
                entity.getDeliveryLatitude(),
                entity.getDeliveryLongitude(),
                entity.getTargetService() != null ? entity.getTargetService().getName() : "SERWIS WŁASNY",

                // Ceny
                entity.getTransportPrice(),
                entity.getServicePrice(),
                entity.getTotalPrice(),

                // Daty i status
                entity.getOrderDate(),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getActualPickupTime(),
                entity.getActualDeliveryTime(),

                // Serwis
                entity.getServicePackageCode(),
                entity.getServicePackage() != null ? entity.getServicePackage().getName() : null,
                entity.getServiceStartDate(),
                entity.getServiceCompletionDate(),
                entity.getServiceNotes(),

                // Notatki
                entity.getTransportNotes(),
                entity.getAdditionalNotes(),

                // Metadane
                entity.getLastModifiedBy(),
                entity.getLastModifiedDate(),

                // Dodatkowe
                entity.getEstimatedTime(),
                entity.getServiceDurationDisplay()
        );
    }

    /**
     * Tworzy DTO z zamówienia transportowego
     */
    public static ServiceAndTransportOrdersDto fromTransportOrder(TransportOrder entity) {
        String clientName = extractClientName(entity.getClient());

        return new ServiceAndTransportOrdersDto(
                entity.getId(),
                "TRANSPORT",

                // Rower
                entity.getBicycle() != null ? entity.getBicycle().getId() : null,
                entity.getBicycle() != null ? entity.getBicycle().getBrand() : null,
                entity.getBicycle() != null ? entity.getBicycle().getModel() : null,
                entity.getBicycle() != null ? entity.getBicycle().getType() : null,

                // Klient
                entity.getClient() != null ? entity.getClient().getEmail() : null,
                entity.getClient() != null ? entity.getClient().getPhoneNumber() : null,
                clientName,

                // Odbiór
                entity.getPickupDate(),
                entity.getPickupAddress(),
                entity.getPickupLatitude(),
                entity.getPickupLongitude(),
                entity.getPickupTimeFrom(),
                entity.getPickupTimeTo(),

                // Dostawa
                entity.getDeliveryAddress(),
                entity.getDeliveryLatitude(),
                entity.getDeliveryLongitude(),
                entity.getTargetService() != null ? entity.getTargetService().getName() : null,

                // Ceny
                entity.getTransportPrice(),
                null, // servicePrice - null dla czystego transportu
                entity.getTotalPrice(),

                // Daty i status
                entity.getOrderDate(),
                entity.getStatus() != null ? entity.getStatus().toString() : null,
                entity.getActualPickupTime(),
                entity.getActualDeliveryTime(),

                // Serwis - wszystkie null dla transportu
                null, null, null, null, null,

                // Notatki
                entity.getTransportNotes(),
                entity.getAdditionalNotes(),

                // Metadane
                entity.getLastModifiedBy(),
                entity.getLastModifiedDate(),

                // Dodatkowe
                entity.getEstimatedTime(),
                null // serviceDurationDisplay - null dla transportu
        );
    }

    /**
     * Pomocnicza metoda do wyciągania nazwy klienta
     */
    private static String extractClientName(com.samarama.bicycle.api.model.IncompleteUser client) {
        if (client instanceof com.samarama.bicycle.api.model.User) {
            com.samarama.bicycle.api.model.User user = (com.samarama.bicycle.api.model.User) client;
            if (user.getFirstName() != null || user.getLastName() != null) {
                String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                String lastName = user.getLastName() != null ? user.getLastName() : "";
                return (firstName + " " + lastName).trim();
            }
        }
        return null;
    }

    /**
     * Sprawdza czy zamówienie można modyfikować/usuwać (tylko PENDING i CONFIRMED)
     */
    public boolean isEditable() {
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }

    /**
     * Sprawdza czy zamówienie można anulować
     */
    public boolean isCancellable() {
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }

    /**
     * Sprawdza czy to zamówienie serwisowe
     */
    public boolean isServiceOrder() {
        return "SERVICE".equals(orderType);
    }

    /**
     * Sprawdza czy to czysto transportowe
     */
    public boolean isTransportOnly() {
        return "TRANSPORT".equals(orderType);
    }

    /**
     * Zwraca czytelną nazwę typu zamówienia
     */
    public String getOrderTypeDisplayName() {
        return switch (orderType) {
            case "SERVICE" -> "Serwis";
            case "TRANSPORT" -> "Transport";
            default -> orderType;
        };
    }

    /**
     * Zwraca czytelną nazwę statusu
     */
    public String getStatusDisplayName() {
        if (status == null) return "Nieznany";

        return switch (status) {
            case "PENDING" -> "Oczekujące";
            case "CONFIRMED" -> "Potwierdzone";
            case "PICKED_UP" -> "Odebrane";
            case "IN_SERVICE" -> "W serwisie";
            case "ON_THE_WAY_BACK" -> "W drodze powrotnej";
            case "CANCELLED" -> "Anulowane";
            default -> status;
        };
    }

    /**
     * Zwraca informację o kliencie (imię + email lub sam email)
     */
    public String getClientDisplayInfo() {
        StringBuilder info = new StringBuilder();

        if (clientName != null && !clientName.trim().isEmpty()) {
            info.append(clientName);
        }

        if (clientEmail != null) {
            if (info.length() > 0) {
                info.append(" (").append(clientEmail).append(")");
            } else {
                info.append(clientEmail);
            }
        }

        return info.toString();
    }

    /**
     * Zwraca opis roweru
     */
    public String getBicycleDescription() {
        StringBuilder desc = new StringBuilder();

        if (bicycleBrand != null) {
            desc.append(bicycleBrand);
        }

        if (bicycleModel != null) {
            if (desc.length() > 0) desc.append(" ");
            desc.append(bicycleModel);
        }

        if (bicycleType != null) {
            if (desc.length() > 0) desc.append(" (");
            desc.append(bicycleType);
            if (desc.indexOf("(") > -1) desc.append(")");
        }

        return desc.length() > 0 ? desc.toString() : "Rower ID: " + bicycleId;
    }

    /**
     * Zwraca informacje o odbiorze
     */
    public String getPickupInfo() {
        StringBuilder info = new StringBuilder();

        if (pickupDate != null) {
            info.append(pickupDate);
        }

        if (pickupTimeFrom != null && pickupTimeTo != null) {
            if (info.length() > 0) info.append(" ");
            info.append("(").append(pickupTimeFrom).append("-").append(pickupTimeTo).append(")");
        }

        return info.toString();
    }

    /**
     * Zwraca szczegółowy opis statusu zamówienia
     */
    public String getDetailedStatusDescription() {
        if (!isServiceOrder()) {
            return getTransportStatusDescription();
        } else {
            return getServiceStatusDescription();
        }
    }

    private String getTransportStatusDescription() {
        return switch (status) {
            case "PENDING" -> "Zamówienie transportowe oczekuje na potwierdzenie";
            case "CONFIRMED" -> "Transport potwierdzony, oczekuje na odbiór";
            case "PICKED_UP" -> "Rower odebrany, w drodze do serwisu";
            case "ON_THE_WAY_BACK" -> "Transport zakończony, rower w drodze powrotnej";
            case "CANCELLED" -> "Transport anulowany";
            default -> "Status: " + getStatusDisplayName();
        };
    }

    private String getServiceStatusDescription() {
        return switch (status) {
            case "PENDING" -> "Zamówienie serwisowe oczekuje na potwierdzenie";
            case "CONFIRMED" -> "Zamówienie potwierdzone, oczekuje na odbiór";
            case "PICKED_UP" -> "Rower odebrany, oczekuje na rozpoczęcie serwisu";
            case "IN_SERVICE" -> "Rower w serwisie" +
                    (serviceStartDate != null ? " od " + serviceStartDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm")) : "");
            case "ON_THE_WAY_BACK" -> "Serwis zakończony" +
                    (serviceDurationDisplay != null ? " (czas serwisu: " + serviceDurationDisplay + ")" : "") +
                    ", rower w drodze powrotnej";
            case "CANCELLED" -> "Zamówienie serwisowe anulowane";
            default -> "Status: " + getStatusDisplayName();
        };
    }

    /**
     * Sprawdza czy zamówienie ma okno czasowe odbioru
     */
    public boolean hasPickupTimeWindow() {
        return pickupTimeFrom != null && pickupTimeTo != null;
    }

    /**
     * Sprawdza czy zamówienie serwisowe zostało rozpoczęte
     */
    public boolean isServiceStarted() {
        return isServiceOrder() && serviceStartDate != null;
    }

    /**
     * Sprawdza czy zamówienie serwisowe zostało zakończone
     */
    public boolean isServiceCompleted() {
        return isServiceOrder() && serviceCompletionDate != null;
    }

    /**
     * Zwraca progress zamówienia w procentach (0-100)
     */
    public int getProgressPercentage() {
        if (status == null) return 0;

        return switch (status) {
            case "PENDING" -> 0;
            case "CONFIRMED" -> 20;
            case "PICKED_UP" -> isServiceOrder() ? 40 : 80;
            case "IN_SERVICE" -> 60;
            case "ON_THE_WAY_BACK" -> 90;
            case "CANCELLED" -> 0;
            default -> 0;
        };
    }

    /**
     * Zwraca informacje o opóźnieniach (jeśli są)
     */
    public String getDelayInfo() {
        if (pickupDate == null) return null;

        LocalDate today = LocalDate.now();

        // Sprawdź opóźnienie w odbiorze
        if ("PENDING".equals(status) || "CONFIRMED".equals(status)) {
            if (pickupDate.isBefore(today)) {
                long daysLate = java.time.temporal.ChronoUnit.DAYS.between(pickupDate, today);
                return "Opóźnienie odbioru: " + daysLate + " dni";
            }
        }

        // Sprawdź długo trwający serwis
        if (isServiceOrder() && "IN_SERVICE".equals(status) && serviceStartDate != null) {
            LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
            if (serviceStartDate.isBefore(weekAgo)) {
                long daysInService = java.time.temporal.ChronoUnit.DAYS.between(serviceStartDate.toLocalDate(), today);
                return "Długi serwis: " + daysInService + " dni";
            }
        }

        return null;
    }

    /**
     * Zwraca prioryteckie zamówienia (wymagające uwagi)
     */
    public boolean isHighPriority() {
        String delayInfo = getDelayInfo();
        return delayInfo != null ||
                ("PICKED_UP".equals(status) && isServiceOrder() && serviceStartDate == null) ||
                ("IN_SERVICE".equals(status) && serviceStartDate != null &&
                        serviceStartDate.isBefore(LocalDateTime.now().minusDays(3)));
    }
}
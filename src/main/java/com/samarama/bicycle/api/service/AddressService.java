package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.AddressDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Serwis do zarządzania adresami użytkowników
 */
public interface AddressService {

    /**
     * Pobiera wszystkie aktywne adresy użytkownika
     * @param userId ID użytkownika
     * @return lista adresów użytkownika
     */
    List<AddressDto> getUserAddresses(Long userId);

    /**
     * Pobiera adres po ID dla konkretnego użytkownika
     * @param addressId ID adresu
     * @param userId ID użytkownika
     * @return adres lub 404 jeśli nie znaleziono
     */
    ResponseEntity<AddressDto> getAddressById(Long addressId, Long userId);

    /**
     * Tworzy nowy adres dla użytkownika
     * @param addressDto dane adresu
     * @param userId ID użytkownika
     * @return odpowiedź z wynikiem operacji
     */
    ResponseEntity<?> createAddress(AddressDto addressDto, Long userId);

    /**
     * Aktualizuje istniejący adres użytkownika
     * @param addressId ID adresu
     * @param addressDto nowe dane adresu
     * @param userId ID użytkownika
     * @return odpowiedź z wynikiem operacji
     */
    ResponseEntity<?> updateAddress(Long addressId, AddressDto addressDto, Long userId);

    /**
     * Usuwa adres użytkownika (soft delete)
     * @param addressId ID adresu
     * @param userId ID użytkownika
     * @return odpowiedź z wynikiem operacji
     */
    ResponseEntity<?> deleteAddress(Long addressId, Long userId);

    /**
     * Pobiera istniejący adres lub tworzy nowy
     * @param addressId ID istniejącego adresu (może być null)
     * @param newAddressData dane nowego adresu (używane gdy addressId nie istnieje)
     * @param userId ID użytkownika
     * @return istniejący lub nowo utworzony adres
     */
    ResponseEntity<AddressDto> getOrCreateAddress(Long addressId, AddressDto newAddressData, Long userId);
}
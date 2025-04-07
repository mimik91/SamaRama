package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.BicycleDto;
import com.samarama.bicycle.api.model.Bicycle;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BicycleService {
    /**
     * Get all bicycles for the currently authenticated user
     * @return list of bicycles
     */
    List<Bicycle> getUserBicycles();

    /**
     * Add a new bicycle
     * @param bicycleDto the bicycle data
     * @param isClient indicates if the request comes from a client
     * @param isService indicates if the request comes from a service
     * @return response with the result of the operation
     */
    ResponseEntity<Map<String, Object>> addBicycle(BicycleDto bicycleDto, boolean isClient, boolean isService);

    /**
     * Upload a photo for a bicycle
     * @param id the bicycle ID
     * @param photo the photo file
     * @return response with the result of the operation
     */
    ResponseEntity<?> uploadBicyclePhoto(Long id, MultipartFile photo);

    /**
     * Get a bicycle's photo
     * @param id the bicycle ID
     * @return response with the photo
     */
    ResponseEntity<?> getBicyclePhoto(Long id);

    /**
     * Delete a bicycle
     * @param id the bicycle ID
     * @return response with the result of the operation
     */
    ResponseEntity<?> deleteBicycle(Long id);

    /**
     * Search for a bicycle by frame number
     * @param frameNumber the frame number to search for
     * @return the matching bicycle or not found response
     */
    ResponseEntity<?> searchBicycleByFrameNumber(String frameNumber);

    /**
     * Update a bicycle's frame number
     * @param id the bicycle ID
     * @param frameNumber the new frame number
     * @return response with the result of the operation
     */
    ResponseEntity<?> updateFrameNumber(Long id, String frameNumber);
}
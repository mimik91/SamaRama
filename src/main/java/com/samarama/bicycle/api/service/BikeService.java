package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.BicycleDto;
import com.samarama.bicycle.api.dto.IncompleteBikeDto;
import com.samarama.bicycle.api.dto.IncompleteBikeResponseDto;
import com.samarama.bicycle.api.model.Bicycle;
import com.samarama.bicycle.api.model.IncompleteBike;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface BikeService {
    /**
     * Get all bicycles for the currently authenticated user
     * @return list of all bikes (incomplete and complete)
     */
    List<IncompleteBikeResponseDto> getAllUserBikes();

    /**
     * Get all incomplete bicycles for the currently authenticated user
     * @return list of incomplete bicycles
     */
    List<IncompleteBike> getUserIncompleteBikes();

    /**
     * Get all complete bicycles for the currently authenticated user
     * @return list of bicycles
     */
    List<Bicycle> getUserBicycles();

    /**
     * Add a new incomplete bicycle
     * @param incompleteBikeDto the incomplete bicycle data
     * @return response with the result of the operation
     */
    ResponseEntity<Map<String, Object>> addIncompleteBike(IncompleteBikeDto incompleteBikeDto);

    /**
     * Convert an incomplete bike to a complete bicycle
     * @param incompleteBikeId ID of the incomplete bicycle
     * @param frameNumber the frame number to be assigned
     * @return response with the result of the operation
     */
    ResponseEntity<Map<String, Object>> convertToComplete(Long incompleteBikeId, String frameNumber);

    /**
     * Add a new bicycle (for backward compatibility, called by services only)
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
     * Upload a photo for an incomplete bicycle
     * @param id the incomplete bicycle ID
     * @param photo the photo file
     * @return response with the result of the operation
     */
    ResponseEntity<?> uploadIncompleteBikePhoto(Long id, MultipartFile photo);

    /**
     * Get a bicycle's photo
     * @param id the bicycle ID
     * @return response with the photo
     */
    ResponseEntity<?> getBicyclePhoto(Long id);

    /**
     * Get an incomplete bicycle's photo
     * @param id the incomplete bicycle ID
     * @return response with the photo
     */
    ResponseEntity<?> getIncompleteBikePhoto(Long id);

    /**
     * Delete a bicycle
     * @param id the bicycle ID
     * @return response with the result of the operation
     */
    ResponseEntity<?> deleteBicycle(Long id);

    /**
     * Delete an incomplete bicycle
     * @param id the incomplete bicycle ID
     * @return response with the result of the operation
     */
    ResponseEntity<?> deleteIncompleteBike(Long id);

    /**
     * Get a bicycle by ID
     * @param id the bicycle ID
     * @return response with the bicycle
     */
    ResponseEntity<Bicycle> getBicycleById(Long id);

    /**
     * Get an incomplete bicycle by ID
     * @param id the incomplete bicycle ID
     * @return response with the incomplete bicycle
     */
    ResponseEntity<IncompleteBike> getIncompleteBikeById(Long id);

    /**
     * Get any bike by ID (incomplete or complete)
     * @param id the bike ID
     * @param isComplete whether the bike is complete
     * @return response with the bike
     */
    ResponseEntity<IncompleteBikeResponseDto> getBikeById(Long id, boolean isComplete);

    /**
     * Update a bicycle's details
     * @param id the bicycle ID
     * @param bicycleDto the new bicycle data
     * @return response with the result of the operation
     */
    ResponseEntity<?> updateBicycle(Long id, BicycleDto bicycleDto);

    /**
     * Update an incomplete bicycle's details
     * @param id the incomplete bicycle ID
     * @param incompleteBikeDto the new incomplete bicycle data
     * @return response with the result of the operation
     */
    ResponseEntity<?> updateIncompleteBike(Long id, IncompleteBikeDto incompleteBikeDto);

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

    /**
     * Delete a bicycle's photo
     * @param id the bicycle ID
     * @return response with the result of the operation
     */
    ResponseEntity<?> deleteBicyclePhoto(Long id);

    /**
     * Delete an incomplete bicycle's photo
     * @param id the incomplete bicycle ID
     * @return response with the result of the operation
     */
    ResponseEntity<?> deleteIncompleteBikePhoto(Long id);
}
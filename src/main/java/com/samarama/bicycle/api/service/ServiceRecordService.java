package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.ServiceRecordDto;
import com.samarama.bicycle.api.model.ServiceRecord;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ServiceRecordService {
    /**
     * Get service records for a specific bicycle
     * @param bicycleId ID of the bicycle
     * @return response with list of service records or appropriate error status
     */
    ResponseEntity<List<ServiceRecord>> getBicycleServiceRecords(Long bicycleId);

    /**
     * Add a new service record
     * @param serviceRecordDto service record data
     * @param userEmail email of the current user
     * @return response with operation result
     */
    ResponseEntity<?> addServiceRecord(ServiceRecordDto serviceRecordDto, String userEmail);
}
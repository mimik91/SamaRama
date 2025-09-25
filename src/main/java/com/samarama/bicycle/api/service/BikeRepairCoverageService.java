package com.samarama.bicycle.api.service;

import com.samarama.bicycle.api.dto.BikeRepairCoverageMapDto;
import com.samarama.bicycle.api.dto.ServiceCoverageAssignmentDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface BikeRepairCoverageService {

    BikeRepairCoverageMapDto getAllRepairCoverages();
    ResponseEntity<?> getMyRepairCoverages(String userEmail);
    ResponseEntity<?> assignMyRepairCoverages(Long bikeServiceId, ServiceCoverageAssignmentDto coverageMapDto);}

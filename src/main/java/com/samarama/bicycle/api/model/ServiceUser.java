package com.samarama.bicycle.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Entity
@Table(name = "service_users")
@PrimaryKeyJoinColumn(name = "id")
public class ServiceUser extends User {

    @NotNull
    @Column(name = "bike_service_id")
    private Long bikeServiceId;
}

package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Rozszerzona encja serwisu rowerowego z godzinami otwarcia
 */

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bike_services_registered")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@ToString(exclude = "pricelistCategories")
public class BikeServiceRegistered extends BikeService {

    @OneToMany(mappedBy = "bikeServiceId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, name ASC")
    @JsonManagedReference
    private List<PricelistCategory> pricelistCategories = new ArrayList<>();


    @Size(max = 255)
    @Column(name = "website")
    private String website;

    @Size(max = 1500)
    @Column(name = "description")
    private String description;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "opening_hours_id")
    private OpeningHours openingHours;

    public PricelistCategory addPricelistCategory(String categoryName) {
        PricelistCategory category = new PricelistCategory();
        category.setBikeServiceId(this.getId());
        category.setName(categoryName);
        category.setDisplayOrder(pricelistCategories.size());
        pricelistCategories.add(category);
        return category;
    }

    /**
     * Dodaje kategorię z opisem
     */
    public PricelistCategory addPricelistCategory(String categoryName, String description) {
        PricelistCategory category = addPricelistCategory(categoryName);
        category.setDescription(description);
        return category;
    }

    /**
     * Usuwa kategorię cennika
     */
    public void removePricelistCategory(PricelistCategory category) {
        pricelistCategories.remove(category);
    }

    public PricelistCategory findCategoryByName(String categoryName) {
        return pricelistCategories.stream()
                .filter(category -> category.getName().equals(categoryName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sprawdza czy serwis ma cennik
     */
    public boolean hasPricelist() {
        return !pricelistCategories.isEmpty();
    }


}

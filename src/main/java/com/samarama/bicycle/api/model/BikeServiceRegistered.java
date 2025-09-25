package com.samarama.bicycle.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rozszerzona encja serwisu rowerowego z godzinami otwarcia
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bike_services_registered")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@ToString(exclude = {"pricelistCategories", "servicesPricelistItems", "bikeRepairCoverages"})
public class BikeServiceRegistered extends BikeService {

    @Size(max = 100)
    @Column(name = "suffix")
    private String suffix;

    @Column(name = "contact_person")
    String ContactPerson;

    /**
     * Kategorie cennika używane przez ten serwis
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "bike_service_pricelist_categories",
            joinColumns = @JoinColumn(name = "bike_service_id"),
            inverseJoinColumns = @JoinColumn(name = "pricelist_category_id")
    )
    @OrderBy("name ASC")
    @EqualsAndHashCode.Exclude
    private Set<PricelistCategory> pricelistCategories = new HashSet<>();

    /**
     * Pozycje cennika z cenami i szczegółami specyficznymi dla tego serwisu
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_service_id")
    @OrderBy("displayOrder ASC")
    private List<BikeServicePricelistItem> servicesPricelistItems = new ArrayList<>();

    @Size(max = 500)
    @Column(name = "pricelist_info")
    private String pricelistInfo;

    @Size(max = 500)
    @Column(name = "pricelist_note")
    private String pricelistNote;

    @Size(max = 255)
    @Column(name = "website")
    private String website;

    @Size(max = 1500)
    @Column(name = "description")
    private String description;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "opening_hours_id")
    private OpeningHours openingHours;

    @Size(max = 500)
    @Column(name = "opening_hours_info")
    private String openingHoursInfo;

    @Size(max = 500)
    @Column(name = "opening_hours_note")
    private String openingHoursNote;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "bike_service_repair_coverage",
            joinColumns = @JoinColumn(name = "bike_service_id"),
            inverseJoinColumns = @JoinColumn(name = "bike_service_coverage_id")
    )
    @EqualsAndHashCode.Exclude
    private Set<BikeRepairCoverage> bikeRepairCoverages = new HashSet<>();

    // === METODY ZARZĄDZANIA KATEGORIAMI ===

    /**
     * Dodaje kategorię do serwisu
     */
    public void addPricelistCategory(PricelistCategory category) {
        pricelistCategories.add(category);
        category.getBikeServices().add(this);
    }

    /**
     * Usuwa kategorię z serwisu
     */
    public void removePricelistCategory(PricelistCategory category) {
        pricelistCategories.remove(category);
        category.getBikeServices().remove(this);
    }

    /**
     * Znajduje kategorię po nazwie
     */
    public PricelistCategory findCategoryByName(String categoryName) {
        return pricelistCategories.stream()
                .filter(category -> category.getName().equals(categoryName))
                .findFirst()
                .orElse(null);
    }

    // === METODY ZARZĄDZANIA POZYCJAMI CENNIKA ===

    /**
     * Dodaje pozycję cennika do serwisu z ceną
     */
    public BikeServicePricelistItem addPricelistItem(PricelistItem item, String price) {
        BikeServicePricelistItem servicePricelistItem = new BikeServicePricelistItem(
                this.getId(), item.getId(), price, servicesPricelistItems.size()
        );
        servicesPricelistItems.add(servicePricelistItem);
        return servicePricelistItem;
    }

    /**
     * Dodaje pozycję cennika do serwisu z ceną i notatką
     */
    public BikeServicePricelistItem addPricelistItem(PricelistItem item, String price, String note) {
        BikeServicePricelistItem servicePricelistItem = addPricelistItem(item, price);
        servicePricelistItem.setServiceSpecificNote(note);
        return servicePricelistItem;
    }

    /**
     * Usuwa pozycję cennika z serwisu
     */
    public void removePricelistItem(BikeServicePricelistItem servicePricelistItem) {
        servicesPricelistItems.remove(servicePricelistItem);
    }

    /**
     * Usuwa pozycję cennika z serwisu po ID pozycji
     */
    public void removePricelistItem(Long pricelistItemId) {
        servicesPricelistItems.removeIf(item -> item.getPricelistItemId().equals(pricelistItemId));
    }

    /**
     * Znajduje pozycje cennika dla danej kategorii
     */
    public List<BikeServicePricelistItem> getItemsForCategory(PricelistCategory category) {
        return servicesPricelistItems.stream()
                .filter(serviceItem ->
                        serviceItem.getPricelistItem() != null &&
                                serviceItem.getPricelistItem().getCategories().contains(category)
                )
                .collect(Collectors.toList());
    }

    /**
     * Sprawdza czy serwis ma cennik
     */
    public boolean hasPricelist() {
        return !pricelistCategories.isEmpty() || !servicesPricelistItems.isEmpty();
    }

    /**
     * Sprawdza czy serwis oferuje daną pozycję cennika
     */
    public boolean offersPricelistItem(Long pricelistItemId) {
        return servicesPricelistItems.stream()
                .anyMatch(item -> item.getPricelistItemId().equals(pricelistItemId));
    }

    /**
     * Znajduje cenę dla danej pozycji cennika w tym serwisie
     */
    public String getPriceForItem(Long pricelistItemId) {
        return servicesPricelistItems.stream()
                .filter(item -> item.getPricelistItemId().equals(pricelistItemId))
                .findFirst()
                .map(BikeServicePricelistItem::getPrice)
                .orElse(null);
    }

    // === METODY ZARZĄDZANIA POKRYCIEM NAPRAW ===

    public void addBikeRepairCoverage(BikeRepairCoverage coverage) {
        bikeRepairCoverages.add(coverage);
        coverage.getBikeServices().add(this);
    }

    public void removeBikeRepairCoverage(BikeRepairCoverage coverage) {
        bikeRepairCoverages.remove(coverage);
        coverage.getBikeServices().remove(this);
    }

    /**
     * Pobiera usługi pogrupowane po kategoriach
     */
    public Map<BikeRepairCoverageCategory, List<BikeRepairCoverage>> getCoveragesByCategory() {
        return bikeRepairCoverages.stream()
                .collect(Collectors.groupingBy(BikeRepairCoverage::getCategory));
    }

    /**
     * Sprawdza czy serwis oferuje daną usługę
     */
    public boolean offersCoverage(BikeRepairCoverage coverage) {
        return bikeRepairCoverages.contains(coverage);
    }

    /**
     * Pobiera listę kategorii usług oferowanych przez serwis
     */
    public Set<BikeRepairCoverageCategory> getOfferedCategories() {
        return bikeRepairCoverages.stream()
                .map(BikeRepairCoverage::getCategory)
                .collect(Collectors.toSet());
    }
}
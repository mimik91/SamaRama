package com.samarama.bicycle.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Reprezentuje pakiet serwisowy dostępny w aplikacji.
 * Pakiety serwisowe to predefiniowane zestawy usług o określonej cenie.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_packages")
public class ServicePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Unikalny kod pakietu (np. "BASIC", "EXTENDED", "FULL")
     * Używany do referencji w innych częściach aplikacji
     */
    @Column(name = "code", unique = true, nullable = false)
    private String code;

    /**
     * Nazwa pakietu widoczna dla użytkownika
     */
    @NotBlank
    @Column(name = "name")
    private String name;

    /**
     * Opis pakietu
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Cena pakietu
     */
    @NotNull
    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "service_id")
    private Long serviceId;

    /**
     * Czy pakiet jest aktywny (dostępny do wyboru)
     */
    @Column(name = "active")
    private boolean active = true;

    /**
     * Kolejność wyświetlania pakietu na liście (niższe wartości = wyżej na liście)
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Lista funkcji/usług zawartych w pakiecie
     */
    @ElementCollection
    @CollectionTable(
            name = "service_package_features",
            joinColumns = @JoinColumn(name = "package_id")
    )
    @Column(name = "feature", length = 500)
    private List<String> features = new ArrayList<>();

    public void addFeature(String feature) {
        if (features == null) {
            features = new ArrayList<>();
        }
        features.add(feature);
    }

    public boolean hasFeature(String feature) {
        return features != null && features.contains(feature);
    }
}
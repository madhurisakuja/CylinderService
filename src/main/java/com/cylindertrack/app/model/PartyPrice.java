package com.cylindertrack.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "party_prices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"party_name", "gas_type"}))
public class PartyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PARTY_PRICE_SEQ")
    @SequenceGenerator(name = "PARTY_PRICE_SEQ", sequenceName = "PARTY_PRICE_SEQ", allocationSize = 1)
    private Long id;

    @NotBlank
    @Column(name = "party_name", nullable = false)
    private String partyName;

    @NotBlank
    @Column(name = "gas_type", nullable = false, length = 20)
    private String gasType;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    public void stamp() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }
    public String getGasType() { return gasType; }
    public void setGasType(String gasType) { this.gasType = gasType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

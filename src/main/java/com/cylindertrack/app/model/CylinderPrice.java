package com.cylindertrack.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cylinder_prices")
public class CylinderPrice {

    @Id
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

    public String getGasType() { return gasType; }
    public void setGasType(String gasType) { this.gasType = gasType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

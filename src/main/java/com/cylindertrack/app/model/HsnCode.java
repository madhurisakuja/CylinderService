package com.cylindertrack.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "hsn_codes")
public class HsnCode {

    @Id
    @Column(name = "gas_type", nullable = false, length = 20)
    private String gasType;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "description", length = 100)
    private String description;

    public String getGasType() { return gasType; }
    public void setGasType(String v) { this.gasType = v; }
    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String v) { this.hsnCode = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}

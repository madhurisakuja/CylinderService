package com.cylindertrack.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

@Entity
@Table(name = "cylinder_entries")
public class MainCylinderEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CYLINDER_ID_SEQ")
    @SequenceGenerator(name = "CYLINDER_ID_SEQ", sequenceName = "CYLINDER_ID_SEQ", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "Customer name is required")
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @NotNull(message = "Date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "entry_date", nullable = false)
    private Date date;

    /** Gas type — OXY, CO2, LPG, DA, ARGON, ARGOSHIELD, NITROGEN */
    @NotBlank(message = "Cylinder type is required")
    @Column(name = "ctype", nullable = false)
    private String ctype;

    /**
     * Movement type — FULL (cylinder sent out) or EMPTY (cylinder returned).
     * Separate from ctype so we know the direction of movement.
     */
    @Column(name = "full_type", nullable = false)
    private String fullType = "FULL";

    @NotNull(message = "Cylinder number is required")
    @Min(value = 1, message = "Cylinder number must be positive")
    @Column(name = "cylinder_no", nullable = false)
    private Long cylinderNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }
    public Date getDate() { return date; }
    public void setDate(Date v) { this.date = v; }
    public String getCtype() { return ctype; }
    public void setCtype(String v) { this.ctype = v; }
    public String getFullType() { return fullType; }
    public void setFullType(String v) { this.fullType = (v != null ? v.toUpperCase() : "FULL"); }
    public Long getCylinderNo() { return cylinderNo; }
    public void setCylinderNo(Long v) { this.cylinderNo = v; }
}

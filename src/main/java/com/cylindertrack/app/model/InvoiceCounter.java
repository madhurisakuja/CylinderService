package com.cylindertrack.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "invoice_counter")
public class InvoiceCounter {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Column(name = "current_number", nullable = false)
    private Integer currentNumber = 1;

    /**
     * The fiscal start year when the counter was last reset.
     * e.g. if last reset in April 2025 (FY 2025-26), this is 2025.
     * Used to detect when we cross into a new fiscal year and need to reset to 1.
     */
    @Column(name = "fiscal_start_year", nullable = false)
    private Integer fiscalStartYear = 2025;

    public Integer getId() { return id; }
    public Integer getCurrentNumber() { return currentNumber; }
    public void setCurrentNumber(Integer v) { this.currentNumber = v; }
    public Integer getFiscalStartYear() { return fiscalStartYear; }
    public void setFiscalStartYear(Integer v) { this.fiscalStartYear = v; }
}

package com.cylindertrack.app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BillLineItem {
    private int slNo;
    private String particulars;
    private String hsnCode;
    private String uom;            // NOS, M3, or blank
    private String gstPercent = "18%";
    private int quantity;
    private BigDecimal rate;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmt;
    private BigDecimal sgstAmt;
    private BigDecimal amount;

    public BillLineItem(int slNo, String particulars, String hsnCode, String uom,
                        int quantity, BigDecimal rate) {
        this.slNo          = slNo;
        this.particulars   = particulars;
        this.hsnCode       = hsnCode;
        this.uom           = uom != null ? uom : "";
        this.quantity      = quantity;
        this.rate          = rate;
        this.taxableAmount = rate.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        this.cgstAmt       = taxableAmount.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        this.sgstAmt       = cgstAmt;
        this.amount        = taxableAmount.add(cgstAmt).add(sgstAmt);
    }

    public int getSlNo()                { return slNo; }
    public String getParticulars()      { return particulars; }
    public String getHsnCode()          { return hsnCode; }
    public String getUom()              { return uom; }
    public boolean hasUom()             { return uom != null && !uom.isBlank(); }
    public String getGstPercent()       { return gstPercent; }
    public int getQuantity()            { return quantity; }
    public BigDecimal getRate()         { return rate; }
    public BigDecimal getTaxableAmount(){ return taxableAmount; }
    public BigDecimal getCgstAmt()      { return cgstAmt; }
    public BigDecimal getSgstAmt()      { return sgstAmt; }
    public BigDecimal getAmount()       { return amount; }
}

package com.cylindertrack.app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class BillLineItem {
    private int slNo;
    private String particulars;   // gas type description e.g. "Oxygen Gas"
    private String hsnCode;
    private BigDecimal gstPercent = new BigDecimal("0.18");
    private int quantity;
    private BigDecimal rate;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmt;
    private BigDecimal sgstAmt;
    private BigDecimal amount;    // taxable + cgst + sgst

    public BillLineItem(int slNo, String gasType, String hsnCode,
                        int quantity, BigDecimal rate) {
        this.slNo         = slNo;
        this.particulars  = toParticulars(gasType);
        this.hsnCode      = hsnCode;
        this.quantity     = quantity;
        this.rate         = rate;
        this.taxableAmount= rate.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        this.cgstAmt      = taxableAmount.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        this.sgstAmt      = cgstAmt;
        this.amount       = taxableAmount.add(cgstAmt).add(sgstAmt);
    }

    private String toParticulars(String gasType) {
        return switch (gasType.toUpperCase()) {
            case "OXY"        -> "Oxygen Gas";
            case "CO2"        -> "Carbon Dioxide Gas";
            case "LPG"        -> "LPG Gas";
            case "DA"         -> "Dissolved Acetylene Gas";
            case "ARGON"      -> "Argon Gas";
            case "ARGOSHIELD" -> "Argoshield Gas";
            case "NITROGEN"   -> "Nitrogen Gas";
            default           -> gasType + " Gas";
        };
    }

    public int getSlNo()                { return slNo; }
    public String getParticulars()      { return particulars; }
    public String getHsnCode()          { return hsnCode; }
    public BigDecimal getGstPercent()   { return gstPercent; }
    public int getQuantity()            { return quantity; }
    public BigDecimal getRate()         { return rate; }
    public BigDecimal getTaxableAmount(){ return taxableAmount; }
    public BigDecimal getCgstAmt()      { return cgstAmt; }
    public BigDecimal getSgstAmt()      { return sgstAmt; }
    public BigDecimal getAmount()       { return amount; }
}

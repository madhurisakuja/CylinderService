package com.cylindertrack.app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class BillLineItem {
    private int slNo;
    private String particulars;
    private String cylinderNumbers; // space-separated cylinder nos if available
    private String hsnCode;
    private String gstPercent = "18%";
    private int quantity;
    private BigDecimal rate;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmt;
    private BigDecimal sgstAmt;
    private BigDecimal amount;

    public BillLineItem(int slNo, String gasType, String hsnCode,
                        int quantity, BigDecimal rate, List<Long> cylinderNos) {
        this.slNo          = slNo;
        this.particulars   = toParticulars(gasType);
        this.hsnCode       = hsnCode;
        this.quantity      = quantity;
        this.rate          = rate;
        this.taxableAmount = rate.multiply(BigDecimal.valueOf(quantity))
                                 .setScale(2, RoundingMode.HALF_UP);
        this.cgstAmt       = taxableAmount.multiply(new BigDecimal("0.09"))
                                          .setScale(2, RoundingMode.HALF_UP);
        this.sgstAmt       = cgstAmt;
        this.amount        = taxableAmount.add(cgstAmt).add(sgstAmt);

        if (cylinderNos != null && !cylinderNos.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Long c : cylinderNos) sb.append(c).append(" ");
            this.cylinderNumbers = sb.toString().trim();
        } else {
            this.cylinderNumbers = "";
        }
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
    public String getCylinderNumbers()  { return cylinderNumbers; }
    public String getHsnCode()          { return hsnCode; }
    public String getGstPercent()       { return gstPercent; }
    public int getQuantity()            { return quantity; }
    public BigDecimal getRate()         { return rate; }
    public BigDecimal getTaxableAmount(){ return taxableAmount; }
    public BigDecimal getCgstAmt()      { return cgstAmt; }
    public BigDecimal getSgstAmt()      { return sgstAmt; }
    public BigDecimal getAmount()       { return amount; }
}

package com.cylindertrack.app.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * A single line on a bill — one FULL cylinder movement.
 * Not persisted; built on the fly during bill generation.
 */
public class BillLineItem {
    private Date date;
    private Long cylinderNo;
    private String gasType;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
    private boolean partySpecificPrice;

    public BillLineItem(Date date, Long cylinderNo, String gasType,
                        BigDecimal unitPrice, boolean partySpecificPrice) {
        this.date = date;
        this.cylinderNo = cylinderNo;
        this.gasType = gasType;
        this.unitPrice = unitPrice;
        this.quantity = 1;
        this.lineTotal = unitPrice;
        this.partySpecificPrice = partySpecificPrice;
    }

    public Date getDate() { return date; }
    public Long getCylinderNo() { return cylinderNo; }
    public String getGasType() { return gasType; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public boolean isPartySpecificPrice() { return partySpecificPrice; }
}

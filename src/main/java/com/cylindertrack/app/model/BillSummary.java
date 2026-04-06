package com.cylindertrack.app.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Full bill for a party over a date range.
 * Not persisted; built on demand.
 */
public class BillSummary {
    private String partyName;
    private Date fromDate;
    private Date toDate;
    private List<BillLineItem> lineItems;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;   // entered manually on the bill page
    private BigDecimal grandTotal;
    private String billNumber;           // e.g. INV-2024-001

    public BillSummary(String partyName, Date fromDate, Date toDate,
                       List<BillLineItem> lineItems, BigDecimal discountAmount,
                       String billNumber) {
        this.partyName = partyName;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.lineItems = lineItems;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.subtotal = lineItems.stream()
                .map(BillLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.grandTotal = this.subtotal.subtract(this.discountAmount)
                .max(BigDecimal.ZERO);
        this.billNumber = billNumber;
    }

    public String getPartyName() { return partyName; }
    public Date getFromDate() { return fromDate; }
    public Date getToDate() { return toDate; }
    public List<BillLineItem> getLineItems() { return lineItems; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public String getBillNumber() { return billNumber; }
    public boolean hasDiscount() { return discountAmount.compareTo(BigDecimal.ZERO) > 0; }
}

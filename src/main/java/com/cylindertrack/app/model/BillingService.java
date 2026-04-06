package com.cylindertrack.app.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("100.00");
    private static final DateTimeFormatter BILL_NUM_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    @Autowired private BillingRepository billingRepository;
    @Autowired private CylinderPriceRepository priceRepository;
    @Autowired private PartyPriceRepository partyPriceRepository;

    /**
     * Builds a complete bill for the given party and date range.
     * Discount is applied as a flat amount (not shown to other parties).
     */
    public BillSummary generateBill(String partyName, Date fromDate, Date toDate,
                                    BigDecimal discountAmount) {
        List<Object[]> rows = billingRepository.findBillableEntries(partyName, fromDate, toDate);
        List<BillLineItem> lineItems = new ArrayList<>();

        for (Object[] row : rows) {
            Date date       = (Date)   row[0];
            Long cylinderNo = (Long)   row[1];
            String gasType  = (String) row[2];

            // Resolve price: party-specific first, fall back to default
            BigDecimal price = resolvePrice(partyName, gasType);
            boolean isPartySpecific = partyPriceRepository
                    .findByPartyNameAndGasType(partyName, gasType).isPresent();

            lineItems.add(new BillLineItem(date, cylinderNo, gasType, price, isPartySpecific));
        }

        String billNumber = "INV-" + BILL_NUM_FMT.format(LocalDate.now()) +
                            "-" + Math.abs(partyName.hashCode() % 9000 + 1000);

        log.info("Bill generated for party={} entries={} discount={}",
                partyName, lineItems.size(), discountAmount);

        return new BillSummary(partyName, fromDate, toDate, lineItems, discountAmount, billNumber);
    }

    /**
     * Resolves the effective price for a party + gas type.
     * Party-specific price wins; falls back to default; falls back to ₹100.
     */
    public BigDecimal resolvePrice(String partyName, String gasType) {
        Optional<PartyPrice> partyPrice =
                partyPriceRepository.findByPartyNameAndGasType(partyName, gasType);
        if (partyPrice.isPresent()) return partyPrice.get().getPrice();

        Optional<CylinderPrice> defaultPrice = priceRepository.findByGasType(gasType);
        return defaultPrice.map(CylinderPrice::getPrice).orElse(DEFAULT_PRICE);
    }

    /**
     * Seeds default prices of ₹100 for all gas types if none exist yet.
     */
    public void seedDefaultPricesIfEmpty() {
        if (priceRepository.count() == 0) {
            for (CylinderTypeF type : CylinderTypeF.values()) {
                CylinderPrice p = new CylinderPrice();
                p.setGasType(type.name());
                p.setPrice(DEFAULT_PRICE);
                priceRepository.save(p);
            }
            log.info("Seeded default cylinder prices at ₹100 each");
        }
    }
}

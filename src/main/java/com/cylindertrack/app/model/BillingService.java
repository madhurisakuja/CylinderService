package com.cylindertrack.app.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("100.00");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public static final Map<String, String> STATE_CODES = new LinkedHashMap<>();
    static {
        STATE_CODES.put("01", "Jammu & Kashmir");
        STATE_CODES.put("02", "Himachal Pradesh");
        STATE_CODES.put("03", "Punjab");
        STATE_CODES.put("04", "Chandigarh");
        STATE_CODES.put("05", "Uttarakhand");
        STATE_CODES.put("06", "Haryana");
        STATE_CODES.put("07", "Delhi");
        STATE_CODES.put("08", "Rajasthan");
        STATE_CODES.put("09", "Uttar Pradesh");
        STATE_CODES.put("10", "Bihar");
        STATE_CODES.put("19", "West Bengal");
        STATE_CODES.put("20", "Jharkhand");
        STATE_CODES.put("21", "Odisha");
        STATE_CODES.put("22", "Chhattisgarh");
        STATE_CODES.put("23", "Madhya Pradesh");
        STATE_CODES.put("24", "Gujarat");
        STATE_CODES.put("27", "Maharashtra");
        STATE_CODES.put("29", "Karnataka");
        STATE_CODES.put("32", "Kerala");
        STATE_CODES.put("33", "Tamil Nadu");
        STATE_CODES.put("36", "Telangana");
        STATE_CODES.put("37", "Andhra Pradesh");
    }

    public static String stateNameFromGstin(String gstin) {
        if (gstin == null || gstin.trim().length() < 2) return null;
        return STATE_CODES.get(gstin.trim().substring(0, 2));
    }

    public static String stateCodeFromGstin(String gstin) {
        if (gstin == null || gstin.trim().length() < 2) return null;
        return gstin.trim().substring(0, 2);
    }

    @Autowired private BillingRepository        billingRepository;
    @Autowired private CylinderPriceRepository  priceRepository;
    @Autowired private PartyPriceRepository     partyPriceRepository;
    @Autowired private PartyAccountRepository   partyAccountRepository;
    @Autowired private HsnCodeRepository        hsnCodeRepository;
    @Autowired private InvoiceCounterRepository invoiceCounterRepository;

    /**
     * Generates a single bill for one party. Invoice date = today.
     */
    @Transactional
    public BillSummary generateBill(String partyName, int year, int month,
                                    BigDecimal discountAmount,
                                    BigDecimal securityDeposit,
                                    BigDecimal tcCharge) {
        Date[] range  = monthRange(year, month);
        Date fromDate = range[0];
        Date toDate   = range[1];
        // Invoice date = today, not month end
        Date invoiceDate = Date.from(LocalDate.now(IST).atStartOfDay(IST).toInstant());

        return buildBill(partyName, year, month, fromDate, toDate, invoiceDate,
                         discountAmount, securityDeposit, tcCharge);
    }

    /**
     * Generates bills for ALL parties active in the given month.
     * Each call allocates its own invoice number. Invoice date = today.
     */
    @Transactional
    public List<BillSummary> generateAllBills(int year, int month,
                                              BigDecimal discount,
                                              BigDecimal security,
                                              BigDecimal tc) {
        Date[] range     = monthRange(year, month);
        Date invoiceDate = Date.from(LocalDate.now(IST).atStartOfDay(IST).toInstant());
        List<String> parties = billingRepository.findPartiesWithActivityInMonth(range[0], range[1]);
        List<BillSummary> bills = new ArrayList<>();
        for (String party : parties) {
            bills.add(buildBill(party, year, month, range[0], range[1], invoiceDate,
                                discount, security, tc));
        }
        log.info("Bulk bill generation: {} parties for {}/{}", bills.size(), month, year);
        return bills;
    }

    private BillSummary buildBill(String partyName, int year, int month,
                                  Date fromDate, Date toDate, Date invoiceDate,
                                  BigDecimal discountAmount, BigDecimal securityDeposit,
                                  BigDecimal tcCharge) {

        List<Object[]> rows = billingRepository.findBillableEntriesGrouped(partyName, fromDate, toDate);

        List<BillLineItem> lineItems = new ArrayList<>();
        int sl = 1;
        for (Object[] row : rows) {
            String gasType = (String) row[0];
            int qty        = ((Number) row[1]).intValue();
            BigDecimal rate = resolvePrice(partyName, gasType);
            String hsn      = hsnCodeRepository.findByGasType(gasType)
                                .map(HsnCode::getHsnCode).orElse("");
            // Fetch cylinder numbers for this gas type in this month
            List<Long> cylNos = billingRepository.findCylinderNumbersForBill(
                    partyName, gasType, fromDate, toDate);
            lineItems.add(new BillLineItem(sl++, gasType, hsn, qty, rate, cylNos));
        }

        int billFiscalStart = (month >= 4) ? year : year - 1;
        InvoiceCounter counter = invoiceCounterRepository.findById(1)
            .orElseGet(() -> {
                InvoiceCounter c = new InvoiceCounter();
                c.setFiscalStartYear(billFiscalStart);
                return invoiceCounterRepository.save(c);
            });

        if (!counter.getFiscalStartYear().equals(billFiscalStart)) {
            log.info("New fiscal year — resetting counter from {} to 1", counter.getFiscalStartYear());
            invoiceCounterRepository.resetForNewFiscalYear(billFiscalStart);
            counter.setCurrentNumber(1);
            counter.setFiscalStartYear(billFiscalStart);
        }

        int invoiceNum = counter.getCurrentNumber();
        invoiceCounterRepository.increment();

        String fiscalYear    = billFiscalStart + "-" + String.valueOf(billFiscalStart + 1).substring(2);
        String invoiceNumber = String.format("%02d", invoiceNum) + "/" + fiscalYear;

        PartyAccount account = partyAccountRepository.findByPartyName(partyName).orElse(null);
        if (account != null && account.getGstin() != null && !account.getGstin().isBlank()) {
            if (account.getStateCode() == null || account.getStateCode().isBlank())
                account.setStateCode(stateCodeFromGstin(account.getGstin()));
            if (account.getStateName() == null || account.getStateName().isBlank())
                account.setStateName(stateNameFromGstin(account.getGstin()));
        }

        log.info("Bill: invoice={} party={} items={}", invoiceNumber, partyName, lineItems.size());
        return new BillSummary(partyName, account, invoiceDate, invoiceNumber, fiscalYear,
                               lineItems, discountAmount, securityDeposit, tcCharge);
    }

    public Date[] monthRange(int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last  = first.withDayOfMonth(first.lengthOfMonth());
        return new Date[]{
            Date.from(first.atStartOfDay(IST).toInstant()),
            Date.from(last.atTime(23, 59, 59).atZone(IST).toInstant())
        };
    }

    public BigDecimal resolvePrice(String partyName, String gasType) {
        return partyPriceRepository.findByPartyNameAndGasType(partyName, gasType)
                .map(PartyPrice::getPrice)
                .orElseGet(() -> priceRepository.findByGasType(gasType)
                        .map(CylinderPrice::getPrice)
                        .orElse(DEFAULT_PRICE));
    }

    public void seedDefaultPricesIfEmpty() {
        if (priceRepository.count() == 0) {
            for (CylinderTypeF type : CylinderTypeF.values()) {
                CylinderPrice p = new CylinderPrice();
                p.setGasType(type.name());
                p.setPrice(DEFAULT_PRICE);
                priceRepository.save(p);
            }
            log.info("Seeded default prices at ₹100");
        }
        if (invoiceCounterRepository.count() == 0) {
            InvoiceCounter c = new InvoiceCounter();
            LocalDate now = LocalDate.now(IST);
            c.setFiscalStartYear(now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1);
            invoiceCounterRepository.save(c);
            log.info("Seeded invoice counter at 01");
        }
        seedHsnCodesIfEmpty();
    }

    private void seedHsnCodesIfEmpty() {
        if (hsnCodeRepository.count() == 0) {
            Map<String, String[]> hsn = new LinkedHashMap<>();
            hsn.put("OXY",        new String[]{"28044090", "OXYGEN"});
            hsn.put("LPG",        new String[]{"27111900", "LPG 19KG"});
            hsn.put("DA",         new String[]{"29012910", "DA Gas"});
            hsn.put("ARGON",      new String[]{"997319",   "SAC"});
            hsn.put("NITROGEN",   new String[]{"28043000", "NITROGEN"});
            hsn.put("ARGOSHIELD", new String[]{"28042100", "ARGOSHIELD"});
            hsn.put("CO2",        new String[]{"28112190", "CO2"});
            hsn.forEach((type, vals) -> {
                HsnCode h = new HsnCode();
                h.setGasType(type);
                h.setHsnCode(vals[0]);
                h.setDescription(vals[1]);
                hsnCodeRepository.save(h);
            });
            log.info("Seeded HSN codes");
        }
    }
}

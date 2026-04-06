package com.cylindertrack.app.controller;

import com.cylindertrack.app.model.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    @Autowired private BillingService billingService;
    @Autowired private CylinderPriceRepository priceRepository;
    @Autowired private PartyPriceRepository partyPriceRepository;
    @Autowired private PartyNamesRepository partyNamesRepository;

    // ── Price management ──────────────────────────────────────────────────────

    @GetMapping("/prices")
    public String pricesPage(Model model) {
        List<CylinderPrice> defaults = priceRepository.findAll();
        // Ensure all gas types are represented even if not yet in DB
        Map<String, BigDecimal> priceMap = new LinkedHashMap<>();
        for (CylinderTypeF t : CylinderTypeF.values()) {
            priceMap.put(t.name(), new BigDecimal("100.00"));
        }
        defaults.forEach(p -> priceMap.put(p.getGasType(), p.getPrice()));

        model.addAttribute("priceMap", priceMap);
        model.addAttribute("partyNames", partyNamesRepository.getAllPartyNames());
        model.addAttribute("partyPrices", partyPriceRepository.findAll());
        model.addAttribute("gasTypes", Arrays.stream(CylinderTypeF.values()).map(Enum::name).toList());
        return "billing/prices";
    }

    @PostMapping("/prices/default")
    public RedirectView updateDefaultPrice(
            @RequestParam String gasType,
            @RequestParam BigDecimal price,
            RedirectAttributes ra) {
        CylinderPrice cp = priceRepository.findByGasType(gasType)
                .orElseGet(() -> { CylinderPrice n = new CylinderPrice(); n.setGasType(gasType); return n; });
        cp.setPrice(price);
        priceRepository.save(cp);
        log.info("Default price updated: {} = ₹{}", gasType, price);
        ra.addFlashAttribute("priceSuccess", "Default price for " + gasType + " updated to ₹" + price);
        return new RedirectView("/billing/prices", true);
    }

    @PostMapping("/prices/party")
    public RedirectView updatePartyPrice(
            @RequestParam String partyName,
            @RequestParam String gasType,
            @RequestParam BigDecimal price,
            RedirectAttributes ra) {
        PartyPrice pp = partyPriceRepository
                .findByPartyNameAndGasType(partyName, gasType)
                .orElseGet(() -> {
                    PartyPrice n = new PartyPrice();
                    n.setPartyName(partyName);
                    n.setGasType(gasType);
                    return n;
                });
        pp.setPrice(price);
        partyPriceRepository.save(pp);
        log.info("Party price updated: {} / {} = ₹{}", partyName, gasType, price);
        ra.addFlashAttribute("priceSuccess",
            "Price for " + partyName + " / " + gasType + " set to ₹" + price);
        return new RedirectView("/billing/prices", true);
    }

    @PostMapping("/prices/party/delete")
    public RedirectView deletePartyPrice(
            @RequestParam Long id,
            RedirectAttributes ra) {
        partyPriceRepository.deleteById(id);
        ra.addFlashAttribute("priceSuccess", "Party-specific price removed.");
        return new RedirectView("/billing/prices", true);
    }

    // ── Bill generation ───────────────────────────────────────────────────────

    @GetMapping("/generate")
    public String billForm(Model model, HttpServletRequest request) {
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        if (flash != null) {
            model.addAttribute("bill", flash.get("bill"));
            model.addAttribute("billError", flash.get("billError"));
        }
        model.addAttribute("partyNames", partyNamesRepository.getAllPartyNames());
        return "billing/generate";
    }

    @PostMapping("/generate")
    public RedirectView generateBill(
            @RequestParam String partyName,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fromDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date toDate,
            @RequestParam(required = false, defaultValue = "0") BigDecimal discount,
            RedirectAttributes ra) {

        if (fromDate.after(toDate)) {
            ra.addFlashAttribute("billError", "From date must be before To date.");
            return new RedirectView("/billing/generate", true);
        }

        BillSummary bill = billingService.generateBill(partyName, fromDate, toDate, discount);

        if (bill.getLineItems().isEmpty()) {
            ra.addFlashAttribute("billError",
                "No FULL cylinder entries found for " + partyName +
                " in the selected date range.");
            return new RedirectView("/billing/generate", true);
        }

        ra.addFlashAttribute("bill", bill);
        return new RedirectView("/billing/generate", true);
    }
}
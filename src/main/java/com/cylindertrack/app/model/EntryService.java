package com.cylindertrack.app.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.*;

/**
 * Handles bulk entry save logic with holding calculation.
 * Mirrors the HoldingSpecs MainFController logic, extracted to a service.
 */
@Service
public class EntryService {

    private static final Logger log = LoggerFactory.getLogger(EntryService.class);

    @Autowired private MainEntryRepository mainEntryRepository;
    @Autowired private NewCylinderFService  cylinderEntryRepo;

    /**
     * Saves a sales entry (isPurchase=false).
     * Calculates new holding = previous holding + full - empty.
     * Returns null if back-dated (caller should reject).
     */
    @Transactional
    public MainEntry saveSalesEntry(MainEntry entry) {
        entry.setIsPurchase(false);
        Date lastDate = mainEntryRepository.getLastDateEntry(entry.getPartyName(), entry.getCtype());
        if (lastDate != null && lastDate.after(entry.getDate())) {
            return null; // back-date — caller handles
        }
        Integer prev = mainEntryRepository.getHoldingDetails(
            entry.getPartyName(), entry.getCtype(), false);
        int holding = (prev != null ? prev : 0) + entry.getCfull() - entry.getCempty();
        entry.setCholding(holding);
        return mainEntryRepository.saveAndFlush(entry);
    }

    /**
     * Saves a purchase entry (isPurchase=true).
     * Holding for purchaser = sum(empty) - sum(full).
     */
    @Transactional
    public MainEntry savePurchaseEntry(MainEntry entry) {
        entry.setIsPurchase(true);
        Date lastDate = mainEntryRepository.getLastDateEntry(entry.getPartyName(), entry.getCtype());
        if (lastDate != null && lastDate.after(entry.getDate())) {
            return null;
        }
        Integer prev = mainEntryRepository.getHoldingDetailsForPurchase(
            entry.getPartyName(), entry.getCtype(), true);
        int holding = (prev != null ? prev : 0) + entry.getCempty() - entry.getCfull();
        entry.setCholding(holding);
        return mainEntryRepository.saveAndFlush(entry);
    }

    /**
     * Returns cylinders of a given type last sent FULL to a party,
     * which are candidates for the EMPTY return dropdown.
     */
    public List<Long> getCylindersFullAtParty(String partyName, String gasType) {
        return cylinderEntryRepo.findCylindersFullAtParty(partyName, gasType);
    }
}

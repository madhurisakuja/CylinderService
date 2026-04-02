package com.cylindertrack.app.controller;

import com.cylindertrack.app.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Controller
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private NewCylinderFService cylinderService;

    @Autowired
    private PartyNamesRepository partyNamesRepository;

    // ── Login ─────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // ── Home ──────────────────────────────────────────────────────────────────

    @GetMapping({"/", "/newhome", "/myhome"})
    public String goHome() {
        return "newhome";
    }

    // ── Party management ──────────────────────────────────────────────────────

    @GetMapping("/newPartyEntryF")
    public String newPartyEntryGet(Model model) {
        model.addAttribute("entryParty", new PartyNames());
        return "newPartyEntryF";
    }

    @PostMapping("/newPartyEntryF")
    public RedirectView newPartyEntry(
            @Valid @ModelAttribute("entryParty") PartyNames partyName,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("validationError",
                bindingResult.getFieldError("partyName") != null
                    ? bindingResult.getFieldError("partyName").getDefaultMessage()
                    : "Invalid input");
            return new RedirectView("/newPartyEntryF", true);
        }

        List<String> existing = partyNamesRepository.getAllPartyNames();
        boolean isNew = !existing.contains(partyName.getPartyName());
        if (isNew) {
            partyNamesRepository.saveAndFlush(partyName);
            log.info("New party registered: {}", partyName.getPartyName());
        }
        redirectAttributes.addFlashAttribute("entrySuccess", isNew);
        redirectAttributes.addFlashAttribute("partyName", partyName.getPartyName());
        return new RedirectView("/newPartyEntryF", true);
    }

    // ── Cylinder entry ────────────────────────────────────────────────────────

    @GetMapping("/newCylinderEntryF")
    public String newCylinderEntryGet(Model model, HttpServletRequest request) {
        MainCylinderEntry entry = new MainCylinderEntry();

        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        if (flash != null) {
            model.addAttribute("existingType",        flash.get("existingType"));
            model.addAttribute("type",                flash.get("type"));
            model.addAttribute("CylinderNo",          flash.get("CylinderNo"));
            model.addAttribute("missingEntry",        flash.get("missingEntry"));
            model.addAttribute("party1",              flash.get("party1"));
            model.addAttribute("party2",              flash.get("party2"));
            model.addAttribute("SavedEntryCylinderNo",flash.get("SavedEntryCylinderNo"));
            model.addAttribute("validationError",     flash.get("validationError"));
        }

        entry.setCustomerName((String) request.getSession(true).getAttribute("customername"));
        entry.setDate((Date)          request.getSession(true).getAttribute("date"));
        entry.setCtype((String)       request.getSession(true).getAttribute("ctype"));

        populateCylinderForm(model);
        model.addAttribute("entry", entry);
        return "newCylinderEntryF";
    }

    @PostMapping("/newCylinderEntryF")
    public RedirectView performNewCylinderEntry(
            @Valid @ModelAttribute("entry") MainCylinderEntry cylinderEntry,
            BindingResult bindingResult,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("validationError", "Please fill in all required fields correctly.");
            return new RedirectView("/newCylinderEntryF", true);
        }

        request.getSession(true).setAttribute("customername", cylinderEntry.getCustomerName());
        request.getSession(true).setAttribute("date",         cylinderEntry.getDate());
        request.getSession(true).setAttribute("ctype",        cylinderEntry.getCtype());

        String lastKnownStatus = cylinderService.getCylinderStatus(cylinderEntry.getCylinderNo());
        String lastCustomer    = cylinderService.getCylinderHoldingStatus(cylinderEntry.getCylinderNo());

        if (!isNull(lastKnownStatus) && lastKnownStatus.equalsIgnoreCase(cylinderEntry.getCtype())) {
            redirectAttributes.addFlashAttribute("existingType", true);
            redirectAttributes.addFlashAttribute("CylinderNo",   cylinderEntry.getCylinderNo());
            redirectAttributes.addFlashAttribute("type",         lastKnownStatus);
        } else {
            redirectAttributes.addFlashAttribute("existingType", false);
        }

        if (!isNull(lastCustomer)
                && cylinderEntry.getCtype().equalsIgnoreCase("EMPTY")
                && !lastCustomer.equalsIgnoreCase(cylinderEntry.getCustomerName())) {
            redirectAttributes.addFlashAttribute("missingEntry", true);
            redirectAttributes.addFlashAttribute("CylinderNo",   cylinderEntry.getCylinderNo());
            redirectAttributes.addFlashAttribute("party1",       lastCustomer);
            redirectAttributes.addFlashAttribute("party2",       cylinderEntry.getCustomerName());
        }

        MainCylinderEntry saved = cylinderService.saveAndFlush(cylinderEntry);
        log.info("Cylinder entry saved: cylinder={} customer={} type={}",
            saved.getCylinderNo(), saved.getCustomerName(), saved.getCtype());
        redirectAttributes.addFlashAttribute("SavedEntryCylinderNo", saved.getCylinderNo());
        return new RedirectView("/newCylinderEntryF", true);
    }

    // ── Delete entry ──────────────────────────────────────────────────────────

    @GetMapping("/deleteCylinderEntryF")
    public String deleteCylinderEntryGet(Model model, HttpServletRequest request) {
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        if (flash != null) {
            model.addAttribute("deleteSuccess", flash.get("deleteSuccess"));
            model.addAttribute("CylinderNo",    flash.get("CylinderNo"));
            model.addAttribute("date",          flash.get("date"));
        }
        populateCylinderForm(model);
        model.addAttribute("entry", new MainCylinderEntry());
        return "deleteCylinderEntryF";
    }

    @PostMapping("/deleteCylinderEntryF")
    public RedirectView performDeleteCylinderEntry(
            @ModelAttribute("entry") MainCylinderEntry cylinderEntry,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("CylinderNo", cylinderEntry.getCylinderNo());
        redirectAttributes.addFlashAttribute("date",       cylinderEntry.getDate());
        try {
            List<Long> ids = cylinderService.findByDetails(
                    cylinderEntry.getCylinderNo(),
                    cylinderEntry.getDate(),
                    cylinderEntry.getCustomerName(),
                    cylinderEntry.getCtype());
            if (ids.isEmpty()) throw new IllegalArgumentException("No matching entry found");
            cylinderService.deleteAllById(ids);
            log.info("Deleted {} entry/entries for cylinder={}", ids.size(), cylinderEntry.getCylinderNo());
            redirectAttributes.addFlashAttribute("deleteSuccess", true);
        } catch (Exception e) {
            log.warn("Delete failed for cylinder={}: {}", cylinderEntry.getCylinderNo(), e.getMessage());
            redirectAttributes.addFlashAttribute("deleteSuccess", false);
        }
        return new RedirectView("/deleteCylinderEntryF", true);
    }

    // ── History / search ──────────────────────────────────────────────────────

    @GetMapping("/CylinderHistoryF")
    public String cylinderHistoryGet(Model model) {
        model.addAttribute("entry", new MainCylinderEntry());
        return "CylinderHistoryF";
    }

    @PostMapping("/CylinderHistoryF")
    public RedirectView getCylinderHistory(
            @ModelAttribute("entry") MainCylinderEntry entry,
            RedirectAttributes redirectAttributes) {
        try {
            List<List<?>> history = cylinderService.findAllByCylinderNo(entry.getCylinderNo());
            redirectAttributes.addFlashAttribute("cylinderNo",       entry.getCylinderNo());
            redirectAttributes.addFlashAttribute("history",          history);
            redirectAttributes.addFlashAttribute("issueReadingData", false);
        } catch (Exception e) {
            log.error("Error fetching history for cylinder={}", entry.getCylinderNo(), e);
            redirectAttributes.addFlashAttribute("issueReadingData", true);
        }
        return new RedirectView("/searchResultF", true);
    }

    @GetMapping("/notInRotation")
    public RedirectView getCylindersNotInRotation(RedirectAttributes redirectAttributes) {
        LocalDate offsetDate = LocalDate.now().minusDays(15);
        Date dateDate = Date.from(offsetDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        try {
            List<List<?>> history = cylinderService.getCylindersNotInRotation(dateDate);
            redirectAttributes.addFlashAttribute("offsetDate",       dateDate);
            redirectAttributes.addFlashAttribute("history",          history);
            redirectAttributes.addFlashAttribute("issueReadingData", false);
            redirectAttributes.addFlashAttribute("inRotationResult", true);
        } catch (Exception e) {
            log.error("Error fetching not-in-rotation cylinders", e);
            redirectAttributes.addFlashAttribute("issueReadingData", true);
        }
        return new RedirectView("/searchResultF", true);
    }

    @GetMapping("/searchResultF")
    public String searchResultF(Model model, HttpServletRequest request) {
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        if (flash == null) {
            return "redirect:/CylinderHistoryF";
        }
        model.addAttribute("issueReadingData", flash.get("issueReadingData"));
        model.addAttribute("history",          flash.get("history"));
        model.addAttribute("cylinderNo",       flash.get("cylinderNo"));
        model.addAttribute("offsetDate",       flash.get("offsetDate"));
        model.addAttribute("inRotationResult", flash.get("inRotationResult"));
        return "searchResultF";
    }

    // ── CSV export ────────────────────────────────────────────────────────────

    @GetMapping("/exportF")
    public void exportToCSV(
            @RequestParam Long cylinderNo,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        String filename = cylinderNo + "_" + new SimpleDateFormat("dd-MM-yyyy").format(new Date()) + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        List<List<?>> entries = cylinderService.findAllByCylinderNo(cylinderNo);
        ICsvListWriter csvWriter = new CsvListWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);
        csvWriter.writeHeader("Date", "Cylinder Type", "Customer Name");
        CellProcessor[] processors = {new FmtDate("dd-MMM-yy"), new NotNull(), new NotNull()};
        for (List<?> row : entries) {
            csvWriter.write(row, processors);
        }
        csvWriter.close();
        log.info("CSV exported for cylinder={}", cylinderNo);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void populateCylinderForm(Model model) {
        model.addAttribute("types",      Arrays.stream(CylinderTypeF.values()).map(Enum::name).toList());
        model.addAttribute("statuses",   Arrays.stream(CylinderStatus.values()).map(Enum::name).toList());
        model.addAttribute("partyNames", partyNamesRepository.getAllPartyNames());
    }
}

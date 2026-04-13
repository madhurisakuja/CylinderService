package com.cylindertrack.app.model;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

@Component
public class ExcelBillGenerator {

    private static final String BIZ_NAME    = "B.K ENTERPRISES";
    private static final String BIZ_GSTIN   = "GSTIN: 20ALFPS1571A1ZP; STATE CODE: 20; STATE: JHARKHAND.";
    private static final String BIZ_ADDR    = "LALBABA FOUNDRY, BURMAMINES, JAMSHEDPUR. 831007";
    private static final String BIZ_CONTACT = "PH NO:7209583688; EMAIL-BKGASCRANE@GMAIL.COM.";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd.MM.yyyy");

    public byte[] generate(List<BillSummary> bills) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();

        XSSFFont bold   = font(wb, true, 11);
        XSSFFont normal = font(wb, false, 10);
        XSSFFont bigBold= font(wb, true, 14);

        for (BillSummary bill : bills) {
            boolean hasUom = bill.getPartyUom() != null && !bill.getPartyUom().isBlank();

            String raw = sanitize(bill.getPartyAccount() != null ? bill.getPartyAccount().getEffectiveBillingName() : bill.getPartyName()) + " " + bill.getInvoiceNumber().replace("/","-");
            XSSFSheet ws = wb.createSheet(raw.length()>31 ? raw.substring(0,31) : raw);

            // Column widths — shift right if UOM column present
            ws.setColumnWidth(0, 6*256);   // A SL NO
            ws.setColumnWidth(1, 28*256);  // B Particulars
            ws.setColumnWidth(2, 14*256);  // C HSN
            if (hasUom) {
                ws.setColumnWidth(3, 8*256);  // D UOM
                ws.setColumnWidth(4, 8*256);  // E GST%
                ws.setColumnWidth(5, 10*256); // F Qty
                ws.setColumnWidth(6, 12*256); // G Rate
                ws.setColumnWidth(7, 14*256); // H Taxable
                ws.setColumnWidth(8, 14*256); // I CGST
                ws.setColumnWidth(9, 14*256); // J SGST
                ws.setColumnWidth(10,14*256); // K Amount
            } else {
                ws.setColumnWidth(3, 8*256);  // D GST%
                ws.setColumnWidth(4, 10*256); // E Qty
                ws.setColumnWidth(5, 12*256); // F Rate
                ws.setColumnWidth(6, 14*256); // G Taxable
                ws.setColumnWidth(7, 14*256); // H CGST
                ws.setColumnWidth(8, 14*256); // I SGST
                ws.setColumnWidth(9, 14*256); // J Amount
            }

            int r = 0;
            cell(ws.createRow(r++), hasUom?9:8, "ORIGINAL COPY", normal, HorizontalAlignment.RIGHT);
            cell(ws.createRow(r++), 0, BIZ_NAME, bigBold, HorizontalAlignment.LEFT);
            r++; r++;
            cell(ws.createRow(r++), 0, BIZ_GSTIN,   normal, HorizontalAlignment.LEFT);
            cell(ws.createRow(r++), 1, BIZ_ADDR,    normal, HorizontalAlignment.LEFT);
            cell(ws.createRow(r++), 0, BIZ_CONTACT, normal, HorizontalAlignment.LEFT);
            cell(ws.createRow(r++), 3, "TAX INVOICE", bigBold, HorizontalAlignment.CENTER);
            r++;

            Row row10 = ws.createRow(r++);
            cell(row10, 0, "Buyer:", bold, HorizontalAlignment.LEFT);
            cell(row10, hasUom?6:5, "INVOICE NO: "+bill.getInvoiceNumber(), bold, HorizontalAlignment.LEFT);

            Row row11 = ws.createRow(r++);
            cell(row11, 0, bill.getPartyAccount() != null ? bill.getPartyAccount().getEffectiveBillingName() : bill.getPartyName(), bold, HorizontalAlignment.LEFT);
            cell(row11, hasUom?6:5, "INVOICE DATE: "+DATE_FMT.format(bill.getInvoiceDate()), normal, HorizontalAlignment.LEFT);

            PartyAccount pa = bill.getPartyAccount();
            if (pa != null) {
                if (nb(pa.getAddress()))
                    for (String line : splitAddr(pa.getAddress()))
                        cell(ws.createRow(r++), 0, line, normal, HorizontalAlignment.LEFT);
                if (nb(pa.getGstin()))
                    cell(ws.createRow(r++), 0, "GSTIN: "+pa.getGstin(), normal, HorizontalAlignment.LEFT);
                if (nb(pa.getStateCode())) {
                    String sc = "STATE CODE: "+pa.getStateCode();
                    if (nb(pa.getStateName())) sc += "   STATE: "+pa.getStateName();
                    cell(ws.createRow(r++), 0, sc, normal, HorizontalAlignment.LEFT);
                }
            }
            r++;

            // Header row
            XSSFCellStyle hdrStyle = hdrStyle(wb, bold);
            Row hdr = ws.createRow(r++);
            if (hasUom) {
                String[] hdrs = {"SL NO","Particulars","HSN code","UOM","GST %","Quantity","Rate","Taxable","CGST Amt","SGST Amt","AMOUNT"};
                for (int i=0; i<hdrs.length; i++) { XSSFCell c=(XSSFCell)hdr.createCell(i); c.setCellValue(hdrs[i]); c.setCellStyle(hdrStyle); }
            } else {
                String[] hdrs = {"SL NO","Particulars","HSN code","GST %","Quantity","Rate","Taxable","CGST Amt","SGST Amt","AMOUNT"};
                for (int i=0; i<hdrs.length; i++) { XSSFCell c=(XSSFCell)hdr.createCell(i); c.setCellValue(hdrs[i]); c.setCellStyle(hdrStyle); }
            }

            // Sub-header
            Row subHdr = ws.createRow(r++);
            int amtCol = hasUom ? 7 : 6;
            cell(subHdr, amtCol,   "Amount", normal, HorizontalAlignment.CENTER);
            cell(subHdr, amtCol+1, "9%",     normal, HorizontalAlignment.CENTER);
            cell(subHdr, amtCol+2, "9%",     normal, HorizontalAlignment.CENTER);

            XSSFCellStyle numStyle  = numStyle(wb, normal);
            XSSFCellStyle bdrStyle  = bdrStyle(wb, normal);

            List<BillLineItem> items = bill.getLineItems();
            int[] dataRows = new int[items.size()];

            for (int i=0; i<items.size(); i++) {
                BillLineItem item = items.get(i);
                Row dr = ws.createRow(r);
                dataRows[i] = r+1;
                r++;

                cellInt(dr, 0, item.getSlNo(), bdrStyle);
                cellStr(dr, 1, item.getParticulars(), bdrStyle);
                cellStr(dr, 2, ns(item.getHsnCode()), bdrStyle);

                if (hasUom) {
                    cellStr(dr, 3, bill.getPartyUom(), bdrStyle);
                    cellStr(dr, 4, "18%",  bdrStyle);
                    cellInt(dr, 5, item.getQuantity(), bdrStyle);
                    cellNum(dr, 6, item.getRate(), numStyle);
                    int eRow = r;
                    formula(dr, 7, "G"+eRow+"*F"+eRow, numStyle);
                    formula(dr, 8, "H"+eRow+"*0.09",   numStyle);
                    formula(dr, 9, "H"+eRow+"*0.09",   numStyle);
                    formula(dr,10, "J"+eRow+"+I"+eRow+"+H"+eRow, numStyle);
                } else {
                    cellStr(dr, 3, "18%",  bdrStyle);
                    cellInt(dr, 4, item.getQuantity(), bdrStyle);
                    cellNum(dr, 5, item.getRate(), numStyle);
                    int eRow = r;
                    formula(dr, 6, "F"+eRow+"*E"+eRow, numStyle);
                    formula(dr, 7, "G"+eRow+"*0.09",   numStyle);
                    formula(dr, 8, "G"+eRow+"*0.09",   numStyle);
                    formula(dr, 9, "I"+eRow+"+H"+eRow+"+G"+eRow, numStyle);
                }
            }

            r += 3;
            int amountCol = hasUom ? 10 : 9;
            String amtLetter = hasUom ? "K" : "J";

            Row totRow = ws.createRow(r++);
            cell(totRow, amountCol-1, "Total", bold, HorizontalAlignment.RIGHT);
            StringBuilder jSum = new StringBuilder();
            for (int i=0; i<dataRows.length; i++) { if(i>0) jSum.append("+"); jSum.append(amtLetter).append(dataRows[i]); }
            if (jSum.length() > 0) formula(totRow, amountCol, jSum.toString(), numStyle);

            if (bill.hasTc()) {
                Row tc = ws.createRow(r++);
                cell(tc, amountCol-1, "T/C", normal, HorizontalAlignment.RIGHT);
                cellNum(tc, amountCol, bill.getTcCharge(), numStyle);
            }
            if (bill.hasSecurity()) {
                Row sec = ws.createRow(r++);
                cell(sec, amountCol-1, "Security Adj.", normal, HorizontalAlignment.RIGHT);
                cellNum(sec, amountCol, bill.getSecurityDeposit().negate(), numStyle);
            }
            if (bill.hasDiscount()) {
                Row disc = ws.createRow(r++);
                cell(disc, amountCol-1, "Discount", normal, HorizontalAlignment.RIGHT);
                cellNum(disc, amountCol, bill.getDiscountAmount().negate(), numStyle);
            }

            Row net = ws.createRow(r++);
            cell(net, amountCol-1, "Net Total", bold, HorizontalAlignment.RIGHT);
            cellNum(net, amountCol, bill.getGrandTotal(), numStyle);

            r++;
            cell(ws.createRow(r++), 0, "AMOUNT IN WORDS: "+bill.getAmountInWords()+".", bold, HorizontalAlignment.LEFT);
            r++;
            cell(ws.createRow(r++), 0, BIZ_NAME, bold, HorizontalAlignment.LEFT);
            r += 2;
            cell(ws.createRow(r),   0, "AUTHORISED SIGNATORY", normal, HorizontalAlignment.LEFT);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out); wb.close();
        return out.toByteArray();
    }

    // ── style/cell helpers ────────────────────────────────────────────────────

    private XSSFFont font(XSSFWorkbook wb, boolean bold, int size) {
        XSSFFont f = wb.createFont(); f.setBold(bold); f.setFontHeightInPoints((short)size); f.setFontName("Arial"); return f;
    }
    private void cell(Row row, int col, String val, XSSFFont font, HorizontalAlignment align) {
        XSSFWorkbook wb = (XSSFWorkbook)row.getSheet().getWorkbook();
        XSSFCellStyle s = wb.createCellStyle(); s.setFont(font); s.setAlignment(align);
        XSSFCell c = (XSSFCell)row.createCell(col); c.setCellValue(val); c.setCellStyle(s);
    }
    private void cellStr(Row row, int col, String val, XSSFCellStyle s) {
        XSSFCell c = (XSSFCell)row.createCell(col); c.setCellValue(ns(val)); c.setCellStyle(s);
    }
    private void cellInt(Row row, int col, int val, XSSFCellStyle s) {
        XSSFCell c = (XSSFCell)row.createCell(col); c.setCellValue(val); c.setCellStyle(s);
    }
    private void cellNum(Row row, int col, BigDecimal val, XSSFCellStyle s) {
        XSSFCell c = (XSSFCell)row.createCell(col); c.setCellValue(val.doubleValue()); c.setCellStyle(s);
    }
    private void formula(Row row, int col, String f, XSSFCellStyle s) {
        XSSFCell c = (XSSFCell)row.createCell(col); c.setCellFormula(f); c.setCellStyle(s);
    }
    private XSSFCellStyle hdrStyle(XSSFWorkbook wb, XSSFFont font) {
        XSSFCellStyle s = wb.createCellStyle(); s.setFont(font); s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex()); s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);   s.setBorderRight(BorderStyle.THIN);
        return s;
    }
    private XSSFCellStyle numStyle(XSSFWorkbook wb, XSSFFont font) {
        XSSFCellStyle s = wb.createCellStyle(); s.setFont(font); s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00")); return s;
    }
    private XSSFCellStyle bdrStyle(XSSFWorkbook wb, XSSFFont font) {
        XSSFCellStyle s = wb.createCellStyle(); s.setFont(font);
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);   s.setBorderRight(BorderStyle.THIN); return s;
    }
    private boolean nb(String s) { return s!=null && !s.isBlank(); }
    private String  ns(String s) { return s!=null ? s : ""; }
    private String sanitize(String n) { return n.replaceAll("[\\\\/:*?\"<>|\\[\\]]","").trim(); }
    private String[] splitAddr(String a) {
        if (a.length()<=40) return new String[]{a};
        int m = a.lastIndexOf(',',40); if(m<0) m=40;
        String f=a.substring(0,m+1).trim(), rest=a.substring(m+1).trim();
        if (rest.length()<=40) return new String[]{f,rest};
        int m2=rest.lastIndexOf(',',40); if(m2<0) m2=40;
        return new String[]{f,rest.substring(0,m2+1).trim(),rest.substring(m2+1).trim()};
    }
}

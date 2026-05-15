package org.example.reportingservice.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import org.example.reportingservice.dto.reponse.LoanReportResponse;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] exportToPdf(LoanReportResponse report) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // 
        document.add(new Paragraph("RAPPORT DE PRÊTS")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(18)
            .setBold());

        // 
        document.add(new Paragraph("Période: " + report.getPeriodStart().format(DATE_FORMATTER) + 
            " - " + report.getPeriodEnd().format(DATE_FORMATTER))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12));

        document.add(new Paragraph(" "));

        //  des indicateurs
        Table table = new Table(2);
        table.addCell("Indicateur");
        table.addCell("Valeur");

        table.addCell("Total demandes");
        table.addCell(String.valueOf(report.getTotalApplications()));

        table.addCell("Demandes approuvées");
        table.addCell(String.valueOf(report.getApprovedApplications()));

        table.addCell("Prêts actifs");
        table.addCell(String.valueOf(report.getActiveLoans()));

        table.addCell("Montant total décaissé");
        table.addCell(String.format("%,.0f FCFA", report.getTotalDisbursedAmount()));

        table.addCell("Montant total remboursé");
        table.addCell(String.format("%,.0f FCFA", report.getTotalRepaidAmount()));

        table.addCell("Taux d'approbation");
        table.addCell(String.format("%.1f%%", report.getApprovalRate()));

        table.addCell("Taux de remboursement");
        table.addCell(String.format("%.1f%%", report.getRecoveryRate()));

        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Rapport généré le " + LocalDateTime.now().format(DATE_FORMATTER))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER));

        document.close();
        return baos.toByteArray();
    }

    public byte[] exportToExcel(LoanReportResponse report) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Rapport Prêts");

            //  pour l'en-tête
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // -têtes
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Indicateur", "Valeur"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 
            int rowNum = 1;
            rowNum = addRow(sheet, rowNum, "Total demandes", report.getTotalApplications());
            rowNum = addRow(sheet, rowNum, "Demandes approuvées", report.getApprovedApplications());
            rowNum = addRow(sheet, rowNum, "Prêts actifs", report.getActiveLoans());
            rowNum = addRow(sheet, rowNum, "Montant total décaissé", 
                String.format("%.0f", report.getTotalDisbursedAmount()) + " FCFA");
            rowNum = addRow(sheet, rowNum, "Montant total remboursé", 
                String.format("%.0f", report.getTotalRepaidAmount()) + " FCFA");
            rowNum = addRow(sheet, rowNum, "Taux d'approbation", 
                String.format("%.1f%%", report.getApprovalRate()));
            rowNum = addRow(sheet, rowNum, "Taux de remboursement", 
                String.format("%.1f%%", report.getRecoveryRate()));

            //  les colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private int addRow(Sheet sheet, int rowNum, String label, Object value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(String.valueOf(value));
        return rowNum + 1;
    }
}
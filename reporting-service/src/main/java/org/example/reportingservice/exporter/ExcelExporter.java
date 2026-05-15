package org.example.reportingservice.exporter;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
public class ExcelExporter {
    
    private static final String REPORTS_DIRECTORY = "reports/";
    //  DATE_FORMATTER car non utilisé
    
    public String exportToExcel(Map<String, Object> data, String fileName) throws IOException {
        //  le dossier s'il n'existe pas
        java.io.File directory = new java.io.File(REPORTS_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        String filePath = REPORTS_DIRECTORY + fileName + ".xlsx";
        
        try (Workbook workbook = new XSSFWorkbook()) {
            //  les styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            //  dateStyle car non utilisé
            CellStyle numberStyle = createNumberStyle(workbook);
            
            //  principale
            Sheet sheet = workbook.createSheet("Rapport");
            
            //  du rapport
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RAPPORT MICROFINANCEHUB");
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));
            
            //  de génération
            Row dateRow = sheet.createRow(2);
            dateRow.createCell(0).setCellValue("Date de génération: ");
            dateRow.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            
            // 
            if (data.containsKey("startDate") && data.containsKey("endDate")) {
                Row periodRow = sheet.createRow(3);
                periodRow.createCell(0).setCellValue("Période: ");
                periodRow.createCell(1).setCellValue(data.get("startDate") + " à " + data.get("endDate"));
            }
            
            int rowNum = 5;
            
            //  Clients
            if (data.containsKey("totalClients") || data.containsKey("newClients")) {
                Row sectionRow = sheet.createRow(rowNum++);
                sectionRow.createCell(0).setCellValue("STATISTIQUES CLIENTS");
                sectionRow.getCell(0).setCellStyle(headerStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));
                rowNum++;
                
                if (data.containsKey("totalClients")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Total clients:");
                    row.createCell(1).setCellValue(String.valueOf(data.get("totalClients")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("newClients")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Nouveaux clients:");
                    row.createCell(1).setCellValue(String.valueOf(data.get("newClients")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("activeClients")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Clients actifs:");
                    row.createCell(1).setCellValue(String.valueOf(data.get("activeClients")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                rowNum++;
            }
            
            //  Prêts
            if (data.containsKey("totalLoanApplications") || data.containsKey("disbursedLoans")) {
                Row sectionRow = sheet.createRow(rowNum++);
                sectionRow.createCell(0).setCellValue("STATISTIQUES PRÊTS");
                sectionRow.getCell(0).setCellStyle(headerStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));
                rowNum++;
                
                if (data.containsKey("totalLoanApplications")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Demandes de prêt:");
                    row.createCell(1).setCellValue(String.valueOf(data.get("totalLoanApplications")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("approvedLoans")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Prêts approuvés:");
                    row.createCell(1).setCellValue(String.valueOf(data.get("approvedLoans")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("disbursedLoans")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Prêts décaissés:");
                    row.createCell(1).setCellValue(String.valueOf(data.get("disbursedLoans")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("activeLoans")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Prêts actifs:");
                    row.createCell(1).setCellValue(String.valueOf(data.get("activeLoans")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("totalDisbursedAmount")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Montant total décaissé:");
                    row.createCell(1).setCellValue(formatAmount((BigDecimal) data.get("totalDisbursedAmount")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("outstandingAmount")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Encours:");
                    row.createCell(1).setCellValue(formatAmount((BigDecimal) data.get("outstandingAmount")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                rowNum++;
            }
            
            //  Remboursements
            if (data.containsKey("totalRepayments")) {
                Row sectionRow = sheet.createRow(rowNum++);
                sectionRow.createCell(0).setCellValue("STATISTIQUES REMBOURSEMENTS");
                sectionRow.getCell(0).setCellStyle(headerStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));
                rowNum++;
                
                if (data.containsKey("totalRepayments")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Total remboursé:");
                    row.createCell(1).setCellValue(formatAmount((BigDecimal) data.get("totalRepayments")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("repaymentRate")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Taux de recouvrement:");
                    row.createCell(1).setCellValue(String.format("%.2f%%", data.get("repaymentRate")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("overdueAmount")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Montant en retard:");
                    row.createCell(1).setCellValue(formatAmount((BigDecimal) data.get("overdueAmount")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                rowNum++;
            }
            
            //  Taux
            if (data.containsKey("approvalRate") || data.containsKey("defaultRate")) {
                Row sectionRow = sheet.createRow(rowNum++);
                sectionRow.createCell(0).setCellValue("INDICATEURS DE PERFORMANCE");
                sectionRow.getCell(0).setCellStyle(headerStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));
                rowNum++;
                
                if (data.containsKey("approvalRate")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Taux d'approbation:");
                    row.createCell(1).setCellValue(String.format("%.2f%%", data.get("approvalRate")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
                
                if (data.containsKey("defaultRate")) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue("Taux de défaut:");
                    row.createCell(1).setCellValue(String.format("%.2f%%", data.get("defaultRate")));
                    row.getCell(1).setCellStyle(numberStyle);
                }
            }
            
            //  les colonnes
            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }
            
            //  le fichier
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        }
        
        log.info("Excel généré: {}", filePath);
        return filePath;
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
    
    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }
}
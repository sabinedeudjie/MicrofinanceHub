package org.example.reportingservice.exporter;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
public class PdfExporter {
    
    private static final String REPORTS_DIRECTORY = "reports/";
    
    public String exportToPdf(Map<String, Object> data, String fileName) throws IOException {
        //  le dossier s'il n'existe pas
        java.io.File directory = new java.io.File(REPORTS_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        String filePath = REPORTS_DIRECTORY + fileName + ".pdf";
        
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            PdfWriter writer = new PdfWriter(fos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            //  principal
            Paragraph title = new Paragraph("RAPPORT MICROFINANCEHUB")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(title);
            
            //  de génération
            Paragraph dateParagraph = new Paragraph("Date de génération: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(20);
            document.add(dateParagraph);
            
            // 
            if (data.containsKey("startDate") && data.containsKey("endDate")) {
                Paragraph periodParagraph = new Paragraph("Période: " + data.get("startDate") + " à " + data.get("endDate"))
                    .setFontSize(10)
                    .setMarginBottom(20);
                document.add(periodParagraph);
            }
            
            //  Clients
            if (data.containsKey("totalClients") || data.containsKey("newClients")) {
                addSection(document, "STATISTIQUES CLIENTS");
                Table table = createTable(2);
                
                addTableRow(table, "Total clients:", formatValue(data.get("totalClients")));
                addTableRow(table, "Nouveaux clients:", formatValue(data.get("newClients")));
                addTableRow(table, "Clients actifs:", formatValue(data.get("activeClients")));
                addTableRow(table, "Taux de croissance:", formatPercentage(data.get("clientGrowthRate")));
                
                document.add(table);
            }
            
            //  Prêts
            if (data.containsKey("totalLoanApplications") || data.containsKey("disbursedLoans")) {
                addSection(document, "STATISTIQUES PRÊTS");
                Table table = createTable(2);
                
                addTableRow(table, "Demandes de prêt:", formatValue(data.get("totalLoanApplications")));
                addTableRow(table, "Prêts approuvés:", formatValue(data.get("approvedLoans")));
                addTableRow(table, "Prêts rejetés:", formatValue(data.get("rejectedLoans")));
                addTableRow(table, "Prêts décaissés:", formatValue(data.get("disbursedLoans")));
                addTableRow(table, "Prêts actifs:", formatValue(data.get("activeLoans")));
                addTableRow(table, "Prêts terminés:", formatValue(data.get("completedLoans")));
                addTableRow(table, "Prêts en défaut:", formatValue(data.get("defaultedLoans")));
                addTableRow(table, "Montant total décaissé:", formatAmount(data.get("totalDisbursedAmount")));
                addTableRow(table, "Encours:", formatAmount(data.get("outstandingAmount")));
                addTableRow(table, "Taille moyenne des prêts:", formatAmount(data.get("averageLoanSize")));
                addTableRow(table, "Taux d'approbation:", formatPercentage(data.get("approvalRate")));
                addTableRow(table, "Taux de défaut:", formatPercentage(data.get("defaultRate")));
                addTableRow(table, "Taux de recouvrement:", formatPercentage(data.get("recoveryRate")));
                
                document.add(table);
            }
            
            //  Remboursements
            if (data.containsKey("totalRepayments")) {
                addSection(document, "STATISTIQUES REMBOURSEMENTS");
                Table table = createTable(2);
                
                addTableRow(table, "Total remboursé:", formatAmount(data.get("totalRepayments")));
                addTableRow(table, "Nombre de transactions:", formatValue(data.get("totalTransactions")));
                addTableRow(table, "Taux de remboursement:", formatPercentage(data.get("repaymentRate")));
                addTableRow(table, "Montant en retard:", formatAmount(data.get("overdueAmount")));
                addTableRow(table, "Nombre de retards:", formatValue(data.get("overdueCount")));
                
                document.add(table);
            }
            
            //  de page
            Paragraph footer = new Paragraph("Rapport généré automatiquement par MicroFinanceHub")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30);
            document.add(footer);
            
            document.close();
        }
        
        log.info("PDF généré: {}", filePath);
        return filePath;
    }
    
    private void addSection(Document document, String title) {
        Paragraph sectionTitle = new Paragraph(title)
            .setFontSize(14)
            .setBold()
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setMarginTop(15)
            .setMarginBottom(10)
            .setPadding(5);
        document.add(sectionTitle);
    }
    
    private Table createTable(int numColumns) {
        Table table = new Table(UnitValue.createPercentArray(numColumns));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(10);
        return table;
    }
    
    private void addTableRow(Table table, String label, String value) {
        Cell labelCell = new Cell().add(new Paragraph(label));
        labelCell.setBold();
        labelCell.setBackgroundColor(ColorConstants.WHITE);
        
        Cell valueCell = new Cell().add(new Paragraph(value != null ? value : "-"));
        valueCell.setTextAlignment(TextAlignment.RIGHT);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    private String formatValue(Object value) {
        if (value == null) return "-";
        if (value instanceof Long) return String.format("%,d", value);
        if (value instanceof Integer) return String.format("%,d", value);
        if (value instanceof BigDecimal) return formatAmount(value);
        return String.valueOf(value);
    }
    
    private String formatAmount(Object amount) {
        if (amount == null) return "0 FCFA";
        if (amount instanceof BigDecimal) {
            return String.format("%,.0f FCFA", ((BigDecimal) amount));
        }
        return amount + " FCFA";
    }
    
    private String formatPercentage(Object rate) {
        if (rate == null) return "-";
        return String.format("%.2f%%", rate);
    }
}
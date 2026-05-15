package org.example.reportingservice.model.enums;

public enum ReportType {
    //  temporels
    DAILY,          //  quotidien
    WEEKLY,         //  hebdomadaire
    MONTHLY,        //  mensuel
    QUARTERLY,      //  trimestriel
    ANNUAL,         //  annuel
    
    //  fonctionnels
    DASHBOARD,              //  de bord
    LOAN_PERFORMANCE,       //  des prêts
    CLIENT_PORTFOLIO,       //  clients
    FINANCIAL_STATEMENT,    //  financier
    REPAYMENT_ANALYSIS,     //  des remboursements
    RISK_ASSESSMENT,        //  des risques
    MONTHLY_SUMMARY,        //  mensuel
    QUARTERLY_REPORT,       //  trimestriel détaillé
    ANNUAL_REPORT;          //  annuel détaillé
    
    /**
     * Vérifie si le type de rapport est temporel
     */
    public boolean isTemporal() {
        return this == DAILY || this == WEEKLY || this == MONTHLY || this == QUARTERLY || this == ANNUAL;
    }
    
    /**
     * Vérifie si le type de rapport est fonctionnel
     */
    public boolean isFunctional() {
        return !isTemporal();
    }
    
    /**
     * Retourne la période par défaut pour ce type de rapport
     */
    public String getDefaultPeriod() {
        switch (this) {
            case DAILY: return "1 jour";
            case WEEKLY: return "7 jours";
            case MONTHLY: return "1 mois";
            case QUARTERLY: return "3 mois";
            case ANNUAL: return "12 mois";
            default: return "Période personnalisée";
        }
    }
}
package com.records.pesa.service.transaction.function;

public class AllTransactionsReportModel {
    private String datetime;
    private String transactionType;
    private String category;
    private String entity;
    private String moneyIn;
    private String moneyOut;
    private String transactionCost;

    public AllTransactionsReportModel() {
    }

    public AllTransactionsReportModel(String datetime, String transactionType, String category, String entity, String moneyIn, String moneyOut, String transactionCost) {
        this.datetime = datetime;
        this.transactionType = transactionType;
        this.category = category;
        this.entity = entity;
        this.moneyIn = moneyIn;
        this.moneyOut = moneyOut;
        this.transactionCost = transactionCost;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getMoneyIn() {
        return moneyIn;
    }

    public void setMoneyIn(String moneyIn) {
        this.moneyIn = moneyIn;
    }

    public String getMoneyOut() {
        return moneyOut;
    }

    public void setMoneyOut(String moneyOut) {
        this.moneyOut = moneyOut;
    }

    public String getTransactionCost() {
        return transactionCost;
    }

    public void setTransactionCost(String transactionCost) {
        this.transactionCost = transactionCost;
    }
}

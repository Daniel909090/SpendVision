package com.spendvision.app.models;

public class Expense {

    private String itemName;
    private double amount;
    private String category;
    private String date;

    public Expense() {
    }

    public Expense(String itemName, double amount, String category, String date) {
        this.itemName = itemName;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
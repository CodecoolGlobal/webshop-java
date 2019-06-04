package com.codecool.shop.model;

public class LineItem {
    private int quantity;
    private Product product;
    private double totalPrice;

    public LineItem(Product product) {
        this.quantity = 1;
        this.product = product;
        this.totalPrice = 0;
    }

    public int getQuantity() {
        return quantity;
    }

    public Product getProduct() {
        return product;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
}
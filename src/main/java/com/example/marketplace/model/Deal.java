package com.example.marketplace.model;

public class Deal {
    private final String seller;
    private final String gameTitle;
    private final double oldPrice;
    private final double newPrice;
    private final String createdAt;

    public Deal(String seller, String gameTitle, double oldPrice, double newPrice, String createdAt) {
        this.seller = seller;
        this.gameTitle = gameTitle;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.createdAt = createdAt;
    }

    public String getSeller() {
        return seller;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public double getOldPrice() {
        return oldPrice;
    }

    public double getNewPrice() {
        return newPrice;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}

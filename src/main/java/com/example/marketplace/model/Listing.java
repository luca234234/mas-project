package com.example.marketplace.model;

public class Listing {
    private final int id;
    private final String seller;
    private final int gameId;
    private final String gameTitle;
    private final double price;
    private final boolean active;

    public Listing(int id, String seller, int gameId, String gameTitle, double price, boolean active) {
        this.id = id;
        this.seller = seller;
        this.gameId = gameId;
        this.gameTitle = gameTitle;
        this.price = price;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getSeller() {
        return seller;
    }

    public int getGameId() {
        return gameId;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public double getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }
}

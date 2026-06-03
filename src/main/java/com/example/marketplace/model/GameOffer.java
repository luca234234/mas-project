package com.example.marketplace.model;

public class GameOffer {
    private final int id;
    private final String title;
    private final String genre;
    private final double basePrice;
    private final double rating;
    private final double lowestPrice;

    public GameOffer(int id, String title, String genre, double basePrice, double rating, double lowestPrice) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.basePrice = basePrice;
        this.rating = rating;
        this.lowestPrice = lowestPrice;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getRating() {
        return rating;
    }

    public double getLowestPrice() {
        return lowestPrice;
    }
}

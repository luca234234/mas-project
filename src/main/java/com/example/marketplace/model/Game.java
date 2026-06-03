package com.example.marketplace.model;

public class Game {
    private final int id;
    private final String title;
    private final String genre;
    private final double basePrice;
    private final double rating;

    public Game(int id, String title, String genre, double basePrice, double rating) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.basePrice = basePrice;
        this.rating = rating;
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
}

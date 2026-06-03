package com.example.marketplace.messaging;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MarketCommand implements Serializable {
    public enum Type {
        BUY,
        RECOMMEND
    }

    private final Type type;
    private final String gameTitle;
    private final double maxPrice;
    private final Set<String> preferences;

    private MarketCommand(Type type, String gameTitle, double maxPrice, Set<String> preferences) {
        this.type = type;
        this.gameTitle = gameTitle;
        this.maxPrice = maxPrice;
        this.preferences = preferences == null ? Set.of() : Set.copyOf(preferences);
    }

    public static MarketCommand buy(String gameTitle, double maxPrice) {
        return new MarketCommand(Type.BUY, gameTitle, maxPrice, Set.of());
    }

    public static MarketCommand recommend(String csvGenres) {
        Set<String> preferences = Arrays.stream(csvGenres.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        return new MarketCommand(Type.RECOMMEND, null, 0.0, preferences);
    }

    public Type getType() {
        return type;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public Set<String> getPreferences() {
        return preferences;
    }
}

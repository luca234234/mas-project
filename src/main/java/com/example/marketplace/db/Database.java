package com.example.marketplace.db;

import com.example.marketplace.model.Deal;
import com.example.marketplace.model.Game;
import com.example.marketplace.model.GameOffer;
import com.example.marketplace.model.Listing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public final class Database {
    private static final String DB_URL = "jdbc:sqlite:data/marketplace.db";
    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Random RANDOM = new Random();

    private Database() {
    }

    public static synchronized void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            Files.createDirectories(Path.of("data"));
            createTables();
            seedIfEmpty();
        } catch (Exception e) {
            throw new IllegalStateException("Database initialization failed", e);
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void createTables() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS games (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL UNIQUE," +
                    "genre TEXT NOT NULL," +
                    "base_price REAL NOT NULL," +
                    "rating REAL NOT NULL" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS listings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "seller TEXT NOT NULL," +
                    "game_id INTEGER NOT NULL," +
                    "price REAL NOT NULL," +
                    "active INTEGER NOT NULL DEFAULT 1," +
                    "FOREIGN KEY(game_id) REFERENCES games(id)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS purchases (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "buyer TEXT NOT NULL," +
                    "seller TEXT NOT NULL," +
                    "game_id INTEGER NOT NULL," +
                    "price REAL NOT NULL," +
                    "purchased_at TEXT NOT NULL," +
                    "FOREIGN KEY(game_id) REFERENCES games(id)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS reviews (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "game_id INTEGER NOT NULL," +
                    "critic TEXT NOT NULL," +
                    "score REAL NOT NULL," +
                    "suspicious INTEGER NOT NULL DEFAULT 0," +
                    "created_at TEXT NOT NULL," +
                    "FOREIGN KEY(game_id) REFERENCES games(id)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS reputation (" +
                    "agent TEXT PRIMARY KEY," +
                    "score INTEGER NOT NULL" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS deals (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "seller TEXT NOT NULL," +
                    "game_id INTEGER NOT NULL," +
                    "old_price REAL NOT NULL," +
                    "new_price REAL NOT NULL," +
                    "created_at TEXT NOT NULL," +
                    "FOREIGN KEY(game_id) REFERENCES games(id)" +
                    ")");
        }
    }

    private static void seedIfEmpty() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM games");
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }
        }

        int neonQuest = insertGame("Neon Quest", "RPG", 59.99, 8.4);
        int ancientSiege = insertGame("Ancient Siege", "Strategy", 49.99, 8.0);
        int cozyFarm = insertGame("Cozy Farm", "Simulation", 29.99, 7.8);
        int spaceTrader = insertGame("Space Trader", "Strategy", 39.99, 8.7);
        int cyberDrift = insertGame("Cyber Drift", "Racing", 44.99, 7.5);
        int potionGuild = insertGame("Potion Guild", "Indie", 24.99, 8.2);
        int retroRacer = insertGame("Retro Racer", "Arcade", 19.99, 7.2);

        insertListing("seller-alpha", neonQuest, 54.99);
        insertListing("seller-alpha", ancientSiege, 47.99);
        insertListing("seller-alpha", cozyFarm, 25.99);
        insertListing("seller-alpha", potionGuild, 21.99);

        insertListing("seller-beta", neonQuest, 52.49);
        insertListing("seller-beta", spaceTrader, 36.99);
        insertListing("seller-beta", cyberDrift, 39.99);
        insertListing("seller-beta", retroRacer, 16.99);

        insertListing("seller-gamma", ancientSiege, 44.99);
        insertListing("seller-gamma", cozyFarm, 27.99);
        insertListing("seller-gamma", spaceTrader, 35.49);
        insertListing("seller-gamma", potionGuild, 22.49);

        setReputation("seller-alpha", 100);
        setReputation("seller-beta", 100);
        setReputation("seller-gamma", 100);
    }

    private static int insertGame(String title, String genre, double basePrice, double rating) throws SQLException {
        String sql = "INSERT INTO games(title, genre, base_price, rating) VALUES (?, ?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, genre);
            ps.setDouble(3, basePrice);
            ps.setDouble(4, rating);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
            throw new SQLException("Could not read generated game id");
        }
    }

    private static void insertListing(String seller, int gameId, double price) throws SQLException {
        String sql = "INSERT INTO listings(seller, game_id, price, active) VALUES (?, ?, ?, 1)";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, seller);
            ps.setInt(2, gameId);
            ps.setDouble(3, price);
            ps.executeUpdate();
        }
    }

    public static synchronized List<Game> games() {
        List<Game> games = new ArrayList<>();
        String sql = "SELECT id, title, genre, base_price, rating FROM games ORDER BY title";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                games.add(new Game(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getDouble("base_price"),
                        rs.getDouble("rating")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read games", e);
        }
        return games;
    }

    public static synchronized List<GameOffer> gameOffers() {
        List<GameOffer> offers = new ArrayList<>();
        String sql = "SELECT g.id, g.title, g.genre, g.base_price, g.rating, MIN(l.price) AS lowest_price " +
                "FROM games g LEFT JOIN listings l ON g.id = l.game_id AND l.active = 1 " +
                "GROUP BY g.id, g.title, g.genre, g.base_price, g.rating " +
                "ORDER BY g.title";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                offers.add(new GameOffer(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getDouble("base_price"),
                        rs.getDouble("rating"),
                        rs.getDouble("lowest_price")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read offers", e);
        }
        return offers;
    }

    public static synchronized Optional<Listing> listingFor(String seller, String gameTitle) {
        String sql = "SELECT l.id, l.seller, l.game_id, g.title, l.price, l.active " +
                "FROM listings l JOIN games g ON l.game_id = g.id " +
                "WHERE l.seller = ? AND g.title = ? AND l.active = 1";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, seller);
            ps.setString(2, gameTitle);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new Listing(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getInt(3),
                        rs.getString(4),
                        rs.getDouble(5),
                        rs.getInt(6) == 1
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read listing", e);
        }
        return Optional.empty();
    }

    public static synchronized Optional<Listing> randomListingForSeller(String seller) {
        List<Listing> listings = new ArrayList<>();
        String sql = "SELECT l.id, l.seller, l.game_id, g.title, l.price, l.active " +
                "FROM listings l JOIN games g ON l.game_id = g.id " +
                "WHERE l.seller = ? AND l.active = 1";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, seller);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                listings.add(new Listing(
                        rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getDouble(5), rs.getInt(6) == 1
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read seller listings", e);
        }
        if (listings.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(listings.get(RANDOM.nextInt(listings.size())));
    }

    public static synchronized void updateListingPrice(int listingId, double newPrice) {
        String sql = "UPDATE listings SET price = ? WHERE id = ?";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, roundMoney(newPrice));
            ps.setInt(2, listingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update listing price", e);
        }
    }

    public static synchronized void recordDeal(String seller, int gameId, double oldPrice, double newPrice) {
        String sql = "INSERT INTO deals(seller, game_id, old_price, new_price, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, seller);
            ps.setInt(2, gameId);
            ps.setDouble(3, roundMoney(oldPrice));
            ps.setDouble(4, roundMoney(newPrice));
            ps.setString(5, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record deal", e);
        }
    }

    public static synchronized List<Deal> recentDeals() {
        List<Deal> deals = new ArrayList<>();
        String sql = "SELECT d.seller, g.title, d.old_price, d.new_price, d.created_at " +
                "FROM deals d JOIN games g ON d.game_id = g.id " +
                "ORDER BY d.id DESC LIMIT 15";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                deals.add(new Deal(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4), rs.getString(5)));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read deals", e);
        }
        return deals;
    }

    public static synchronized Optional<Deal> latestDeal() {
        List<Deal> deals = recentDeals();
        if (deals.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(deals.get(0));
    }

    public static synchronized void recordPurchase(String buyer, String seller, int gameId, double price) {
        String sql = "INSERT INTO purchases(buyer, seller, game_id, price, purchased_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, buyer);
            ps.setString(2, seller);
            ps.setInt(3, gameId);
            ps.setDouble(4, roundMoney(price));
            ps.setString(5, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record purchase", e);
        }
    }

    public static synchronized List<Game> recommendations(String buyer, Set<String> preferredGenres) {
        Set<Integer> purchased = purchasedGameIds(buyer);
        List<Game> candidates = games().stream()
                .filter(game -> !purchased.contains(game.getId()))
                .sorted(Comparator
                        .comparing((Game game) -> !preferredGenres.contains(game.getGenre()))
                        .thenComparing(Game::getRating, Comparator.reverseOrder())
                        .thenComparing(Game::getTitle))
                .limit(5)
                .toList();
        return candidates;
    }

    private static Set<Integer> purchasedGameIds(String buyer) {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT DISTINCT game_id FROM purchases WHERE buyer = ?";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, buyer);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read purchase history", e);
        }
        return ids;
    }

    public static synchronized void recordReview(int gameId, String critic, double score, boolean suspicious) {
        String sql = "INSERT INTO reviews(game_id, critic, score, suspicious, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setString(2, critic);
            ps.setDouble(3, score);
            ps.setInt(4, suspicious ? 1 : 0);
            ps.setString(5, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not record review", e);
        }
    }

    public static synchronized void refreshRating(int gameId) {
        String averageSql = "SELECT AVG(score) FROM reviews WHERE game_id = ?";
        String updateSql = "UPDATE games SET rating = ? WHERE id = ?";
        try (Connection connection = connect(); PreparedStatement average = connection.prepareStatement(averageSql)) {
            average.setInt(1, gameId);
            ResultSet rs = average.executeQuery();
            if (rs.next() && rs.getObject(1) != null) {
                double newRating = roundOne(rs.getDouble(1));
                try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                    update.setDouble(1, newRating);
                    update.setInt(2, gameId);
                    update.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not refresh rating", e);
        }
    }

    public static synchronized Optional<Game> randomGame() {
        List<Game> games = games();
        if (games.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(games.get(RANDOM.nextInt(games.size())));
    }

    public static synchronized Optional<Game> gameByTitle(String title) {
        String sql = "SELECT id, title, genre, base_price, rating FROM games WHERE title = ?";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new Game(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getDouble(5)));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read game", e);
        }
        return Optional.empty();
    }

    public static synchronized void changeReputation(String agent, int delta) {
        int current = reputation(agent).orElse(100);
        setReputation(agent, Math.max(0, current + delta));
    }

    public static synchronized Optional<Integer> reputation(String agent) {
        String sql = "SELECT score FROM reputation WHERE agent = ?";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, agent);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read reputation", e);
        }
        return Optional.empty();
    }

    private static void setReputation(String agent, int score) {
        String sql = "INSERT INTO reputation(agent, score) VALUES (?, ?) " +
                "ON CONFLICT(agent) DO UPDATE SET score = excluded.score";
        try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, agent);
            ps.setInt(2, score);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not set reputation", e);
        }
    }

    public static double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String now() {
        return LocalDateTime.now().format(DB_TIME);
    }
}

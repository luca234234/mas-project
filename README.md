# Multi-Agent Video Game Marketplace

A small JADE + Swing + SQLite project for a simulated digital game storefront.

## Agent types

1. BuyerAgent — receives GUI commands, sends Contract Net CFPs, accepts the best seller proposal.
2. SellerPublisherAgent — lists games, proposes prices, records sales, runs promotions.
3. RegulatorAgent — receives sale/review reports, flags suspicious activity, changes reputation.
4. RecommendationAgent — returns personalized suggestions from buyer history and preferences.
5. CriticAgent — generates/aggregates review scores and flags suspicious review patterns.
6. DealAggregatorAgent — collects seller discounts and broadcasts deal notifications.

## Tech stack

- JADE agents
- FIPA ACL messages
- FIPA Contract Net Protocol for buyer/seller negotiation
- Java Swing GUI
- SQLite through JDBC
- Maven

## Run

```bash
mvn clean package
mvn exec:java
```

Alternative after packaging:

```bash
java -jar target/multi-agent-game-marketplace-1.0.0.jar
```

A SQLite database is created automatically at:

```text
data/marketplace.db
```

Delete the `data` folder to reset the demo market.

## GUI usage

- Select a game and press **Buy selected game**.
- Enter a maximum price when prompted.
- Watch the log panel for CFP, proposal, accept/reject, sale, regulation, review, and deal events.
- Press **Recommendations** to ask the RecommendationAgent for personalized game suggestions.
- Press **Refresh** to reload current listings and recent deals.

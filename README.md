# Multi-Agent Video Game Marketplace

## Problem domain

This project simulates a digital video game storefront where autonomous agents buy, sell, regulate, review, recommend, and track game deals in a dynamic marketplace.

## Architecture

The application uses a local JADE multi-agent container with six agent types:

1. BuyerAgent — receives GUI commands, sends Contract Net CFPs, accepts the best seller proposal.
2. SellerPublisherAgent — lists games, proposes prices, records sales, runs promotions.
3. RegulatorAgent — receives sale/review reports, flags suspicious activity, changes reputation.
4. RecommendationAgent — returns personalized suggestions from buyer history and preferences.
5. CriticAgent — generates/aggregates review scores and flags suspicious review patterns.
6. DealAggregatorAgent — collects seller discounts and broadcasts deal notifications.

Agents communicate with FIPA ACL messages. Buyer/seller negotiation uses the FIPA Contract Net Protocol. The Swing GUI sends user actions to the BuyerAgent through JADE O2A objects and shows marketplace data from SQLite. SQLite stores games, listings, purchases, reviews, reputation, and deals.

## Tech stack

- JADE agents
- FIPA ACL messages
- FIPA Contract Net Protocol for buyer/seller negotiation
- Java Swing GUI
- SQLite through JDBC
- Maven

## Starting the project

Requirements:

- Java 17 or newer
- Maven

Run from the project root:

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

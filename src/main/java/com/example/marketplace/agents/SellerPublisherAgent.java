package com.example.marketplace.agents;

import com.example.marketplace.db.Database;
import com.example.marketplace.model.Listing;
import com.example.marketplace.util.Events;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;

import java.util.Optional;
import java.util.Random;

public class SellerPublisherAgent extends Agent {
    private final Random random = new Random();

    @Override
    protected void setup() {
        AgentDirectory.register(this, "game-seller", getLocalName());
        Events.log(getLocalName(), "ready; listings loaded from SQLite");

        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP)
        );
        addBehaviour(new ContractNetResponder(this, template) {
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                return answerCfp(cfp);
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                return completeSale(cfp, propose, accept);
            }

            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                Events.log(getLocalName(), "proposal rejected by " + reject.getSender().getLocalName());
            }
        });

        addBehaviour(new TickerBehaviour(this, 18000 + random.nextInt(8000)) {
            @Override
            protected void onTick() {
                runPromotion();
            }
        });
    }

    private ACLMessage answerCfp(ACLMessage cfp) throws RefuseException {
        String[] parts = cfp.getContent().split("\\|");
        if (parts.length < 2) {
            throw new RefuseException("Malformed CFP");
        }

        String gameTitle = parts[0];
        double maxPrice = Double.parseDouble(parts[1]);
        Optional<Listing> listing = Database.listingFor(getLocalName(), gameTitle);

        if (listing.isEmpty()) {
            throw new RefuseException("Game not listed by seller");
        }

        Listing offer = listing.get();
        if (offer.getPrice() > maxPrice) {
            throw new RefuseException("Price above buyer limit");
        }

        ACLMessage proposal = cfp.createReply();
        proposal.setPerformative(ACLMessage.PROPOSE);
        proposal.setContent(offer.getGameTitle() + "|" + offer.getPrice() + "|" + offer.getGameId());
        Events.log(getLocalName(), "proposed $" + offer.getPrice() + " for " + offer.getGameTitle() + " to " + cfp.getSender().getLocalName());
        return proposal;
    }

    private ACLMessage completeSale(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
        String[] parts = accept.getContent().split("\\|");
        if (parts.length < 3) {
            throw new FailureException("Accepted proposal was malformed");
        }

        String gameTitle = parts[0];
        double price = Double.parseDouble(parts[1]);
        int gameId = Integer.parseInt(parts[2]);
        String buyer = accept.getSender().getLocalName();

        Database.recordPurchase(buyer, getLocalName(), gameId, price);
        applyPostSalePriceAdjustment(gameTitle);

        ACLMessage inform = accept.createReply();
        inform.setPerformative(ACLMessage.INFORM);
        inform.setContent("SOLD|" + gameTitle + "|seller=" + getLocalName() + "|price=$" + price);
        Events.log(getLocalName(), "sold " + gameTitle + " to " + buyer + " for $" + price);
        notifyRegulator(buyer, gameTitle, price);
        return inform;
    }

    private void applyPostSalePriceAdjustment(String gameTitle) {
        Database.listingFor(getLocalName(), gameTitle).ifPresent(listing -> {
            double newPrice = Database.roundMoney(listing.getPrice() * 1.03);
            Database.updateListingPrice(listing.getId(), newPrice);
            Events.log(getLocalName(), "dynamic price update after sale: " + gameTitle + " now $" + newPrice);
        });
    }

    private void notifyRegulator(String buyer, String gameTitle, double price) {
        ACLMessage report = new ACLMessage(ACLMessage.INFORM);
        report.addReceiver(new AID("regulator", AID.ISLOCALNAME));
        report.setOntology("regulation");
        report.setContent("SALE|buyer=" + buyer + "|seller=" + getLocalName() + "|game=" + gameTitle + "|price=" + price);
        send(report);
    }

    private void runPromotion() {
        Database.randomListingForSeller(getLocalName()).ifPresent(listing -> {
            double discount = 0.85 + (random.nextDouble() * 0.10); // 5% to 15% off
            double newPrice = Math.max(4.99, Database.roundMoney(listing.getPrice() * discount));
            if (newPrice >= listing.getPrice()) {
                return;
            }

            Database.updateListingPrice(listing.getId(), newPrice);
            Database.recordDeal(getLocalName(), listing.getGameId(), listing.getPrice(), newPrice);
            Events.log(getLocalName(), "promotion: " + listing.getGameTitle() + " $" + listing.getPrice() + " -> $" + newPrice);

            ACLMessage deal = new ACLMessage(ACLMessage.INFORM);
            deal.addReceiver(new AID("deal-aggregator", AID.ISLOCALNAME));
            deal.setOntology("deal");
            deal.setContent("DEAL|seller=" + getLocalName() + "|game=" + listing.getGameTitle() + "|old=$" + listing.getPrice() + "|new=$" + newPrice);
            send(deal);
        });
    }
}

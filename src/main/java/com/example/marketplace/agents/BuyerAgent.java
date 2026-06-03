package com.example.marketplace.agents;

import com.example.marketplace.messaging.MarketCommand;
import com.example.marketplace.util.Events;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Vector;

public class BuyerAgent extends Agent {
    private double budget = 75.00;
    private Set<String> preferences = Set.of("RPG", "Strategy", "Indie");

    @Override
    protected void setup() {
        setEnabledO2ACommunication(true, 20);
        AgentDirectory.register(this, "buyer", getLocalName());
        Events.log(getLocalName(), "ready with budget $" + budget + " and preferences " + preferences);

        addBehaviour(new TickerBehaviour(this, 500) {
            @Override
            protected void onTick() {
                Object object;
                while ((object = getO2AObject()) != null) {
                    if (object instanceof MarketCommand command) {
                        handleGuiCommand(command);
                    }
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate template = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology("deal")
                );
                ACLMessage message = receive(template);
                if (message == null) {
                    block();
                    return;
                }
                Events.log(getLocalName(), "received deal notice: " + message.getContent());
            }
        });
    }

    private void handleGuiCommand(MarketCommand command) {
        if (command.getType() == MarketCommand.Type.BUY) {
            startPurchase(command.getGameTitle(), command.getMaxPrice());
        } else if (command.getType() == MarketCommand.Type.RECOMMEND) {
            preferences = command.getPreferences().isEmpty() ? preferences : command.getPreferences();
            requestRecommendations();
        }
    }

    private void startPurchase(String gameTitle, double maxPrice) {
        if (maxPrice > budget) {
            Events.log(getLocalName(), "refused GUI buy command: max price exceeds budget ($" + budget + ")");
            return;
        }

        List<AID> sellers = AgentDirectory.search(this, "game-seller");
        if (sellers.isEmpty()) {
            Events.log(getLocalName(), "no sellers found for Contract Net negotiation");
            return;
        }

        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        for (AID seller : sellers) {
            cfp.addReceiver(seller);
        }
        cfp.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        cfp.setConversationId("buy-" + System.currentTimeMillis());
        cfp.setReplyByDate(new Date(System.currentTimeMillis() + 4000));
        cfp.setContent(gameTitle + "|" + maxPrice);

        Events.log(getLocalName(), "sent CFP for " + gameTitle + " with max price $" + maxPrice + " to " + sellers.size() + " sellers");
        addBehaviour(new ContractNetInitiator(this, cfp) {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            protected void handleAllResponses(Vector responses, Vector acceptances) {
                ACLMessage bestProposal = null;
                double bestPrice = Double.MAX_VALUE;

                for (Object object : responses) {
                    ACLMessage response = (ACLMessage) object;
                    if (response.getPerformative() == ACLMessage.PROPOSE) {
                        double price = parseProposalPrice(response.getContent());
                        Events.log(getLocalName(), response.getSender().getLocalName() + " proposed $" + price + " for " + gameTitle);
                        if (price < bestPrice) {
                            bestPrice = price;
                            bestProposal = response;
                        }
                    } else {
                        Events.log(getLocalName(), response.getSender().getLocalName() + " refused CFP for " + gameTitle);
                    }
                }

                for (Object object : responses) {
                    ACLMessage response = (ACLMessage) object;
                    ACLMessage reply = response.createReply();
                    if (response == bestProposal && bestPrice <= maxPrice) {
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContent(response.getContent());
                        Events.log(getLocalName(), "accepted best proposal: " + response.getSender().getLocalName() + " at $" + bestPrice);
                    } else {
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        reply.setContent("Rejected: better offer selected or price too high");
                    }
                    acceptances.add(reply);
                }

                if (bestProposal == null) {
                    Events.log(getLocalName(), "no acceptable proposal received for " + gameTitle);
                }
            }

            @Override
            protected void handleInform(ACLMessage inform) {
                Events.log(getLocalName(), "purchase completed: " + inform.getContent());
            }
        });
    }

    private double parseProposalPrice(String content) {
        String[] parts = content.split("\\|");
        if (parts.length < 2) {
            return Double.MAX_VALUE;
        }
        return Double.parseDouble(parts[1]);
    }

    private void requestRecommendations() {
        List<AID> recommenders = AgentDirectory.search(this, "recommendation");
        if (recommenders.isEmpty()) {
            Events.log(getLocalName(), "no RecommendationAgent found");
            return;
        }

        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(recommenders.get(0));
        request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        request.setConversationId("recommend-" + System.currentTimeMillis());
        request.setContent("RECOMMEND|" + getLocalName() + "|" + join(preferences));

        Events.log(getLocalName(), "requested recommendations for preferences " + preferences);
        addBehaviour(new AchieveREInitiator(this, request) {
            @Override
            protected void handleInform(ACLMessage inform) {
                Events.log(getLocalName(), "recommendations: " + inform.getContent());
            }

            @Override
            protected void handleFailure(ACLMessage failure) {
                Events.log(getLocalName(), "recommendation request failed: " + failure.getContent());
            }
        });
    }

    private String join(Set<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        values.forEach(joiner::add);
        return joiner.toString();
    }
}

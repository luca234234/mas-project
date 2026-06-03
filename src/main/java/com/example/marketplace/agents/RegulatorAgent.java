package com.example.marketplace.agents;

import com.example.marketplace.db.Database;
import com.example.marketplace.model.Game;
import com.example.marketplace.util.Events;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Optional;

public class RegulatorAgent extends Agent {
    @Override
    protected void setup() {
        AgentDirectory.register(this, "regulator", getLocalName());
        Events.log(getLocalName(), "ready; enforcing marketplace rules");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate template = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology("regulation")
                );
                ACLMessage message = receive(template);
                if (message == null) {
                    block();
                    return;
                }
                inspect(message.getContent());
            }
        });
    }

    private void inspect(String content) {
        if (content.startsWith("SALE|")) {
            inspectSale(content);
        } else if (content.startsWith("REVIEW_FLAG|")) {
            inspectReviewFlag(content);
        } else {
            Events.log(getLocalName(), "ignored unknown report: " + content);
        }
    }

    private void inspectSale(String content) {
        String seller = valueOf(content, "seller");
        String gameTitle = valueOf(content, "game");
        double price = Double.parseDouble(valueOf(content, "price"));

        Optional<Game> game = Database.gameByTitle(gameTitle);
        if (game.isPresent() && price > game.get().getBasePrice() * 1.6) {
            Database.changeReputation(seller, -10);
            Events.log(getLocalName(), "flagged sale price manipulation by " + seller + " for " + gameTitle);
        } else {
            Database.changeReputation(seller, 1);
            Events.log(getLocalName(), "sale approved for " + gameTitle + " by " + seller);
        }
    }

    private void inspectReviewFlag(String content) {
        String critic = valueOf(content, "critic");
        String game = valueOf(content, "game");
        Database.changeReputation(critic, -3);
        Events.log(getLocalName(), "review manipulation warning accepted for " + game + "; source=" + critic);
    }

    private String valueOf(String content, String key) {
        String prefix = key + "=";
        String[] parts = content.split("\\|");
        for (String part : parts) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length());
            }
        }
        return "";
    }
}

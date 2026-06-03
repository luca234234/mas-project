package com.example.marketplace.agents;

import com.example.marketplace.db.Database;
import com.example.marketplace.model.Deal;
import com.example.marketplace.util.Events;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;

public class DealAggregatorAgent extends Agent {
    @Override
    protected void setup() {
        AgentDirectory.register(this, "deal-aggregator", getLocalName());
        Events.log(getLocalName(), "ready; tracking discounts and broadcasts");

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
                Events.log(getLocalName(), "indexed seller deal: " + message.getContent());
                broadcast(message.getContent());
            }
        });

        addBehaviour(new TickerBehaviour(this, 30000) {
            @Override
            protected void onTick() {
                Database.latestDeal().ifPresent(deal -> broadcast(format(deal)));
            }
        });
    }

    private void broadcast(String content) {
        List<AID> buyers = AgentDirectory.search(this, "buyer");
        for (AID buyer : buyers) {
            ACLMessage notice = new ACLMessage(ACLMessage.INFORM);
            notice.addReceiver(buyer);
            notice.setOntology("deal");
            notice.setContent(content);
            send(notice);
        }
        if (!buyers.isEmpty()) {
            Events.log(getLocalName(), "broadcast deal to " + buyers.size() + " buyer agent(s)");
        }
    }

    private String format(Deal deal) {
        return "DEAL|seller=" + deal.getSeller() + "|game=" + deal.getGameTitle() + "|old=$" + deal.getOldPrice() + "|new=$" + deal.getNewPrice();
    }
}

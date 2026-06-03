package com.example.marketplace.agents;

import com.example.marketplace.db.Database;
import com.example.marketplace.model.Game;
import com.example.marketplace.util.Events;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RecommendationAgent extends Agent {
    @Override
    protected void setup() {
        AgentDirectory.register(this, "recommendation", getLocalName());
        Events.log(getLocalName(), "ready; using buyer history and genre preferences");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage request = receive(template);
                if (request == null) {
                    block();
                    return;
                }
                answer(request);
            }
        });
    }

    private void answer(ACLMessage request) {
        String[] parts = request.getContent().split("\\|");
        ACLMessage reply = request.createReply();

        if (parts.length < 3 || !"RECOMMEND".equals(parts[0])) {
            reply.setPerformative(ACLMessage.FAILURE);
            reply.setContent("Malformed recommendation request");
            send(reply);
            return;
        }

        String buyer = parts[1];
        Set<String> genres = Arrays.stream(parts[2].split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        String result = Database.recommendations(buyer, genres).stream()
                .map(Game::getTitle)
                .collect(Collectors.joining(", "));

        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(result.isBlank() ? "No new games to recommend" : result);
        Events.log(getLocalName(), "recommended games to " + buyer + ": " + reply.getContent());
        send(reply);
    }
}

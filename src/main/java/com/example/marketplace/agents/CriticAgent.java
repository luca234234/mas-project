package com.example.marketplace.agents;

import com.example.marketplace.db.Database;
import com.example.marketplace.model.Game;
import com.example.marketplace.util.Events;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Optional;
import java.util.Random;

public class CriticAgent extends Agent {
    private final Random random = new Random();

    @Override
    protected void setup() {
        AgentDirectory.register(this, "critic", getLocalName());
        Events.log(getLocalName(), "ready; aggregating reviews and detecting suspicious scores");

        addBehaviour(new TickerBehaviour(this, 14000) {
            @Override
            protected void onTick() {
                reviewRandomGame();
            }
        });
    }

    private void reviewRandomGame() {
        Optional<Game> maybeGame = Database.randomGame();
        if (maybeGame.isEmpty()) {
            return;
        }

        Game game = maybeGame.get();
        double score = Database.roundOne(5.5 + random.nextDouble() * 4.5);
        boolean suspicious = score >= 9.6 && random.nextBoolean();

        Database.recordReview(game.getId(), getLocalName(), score, suspicious);
        Database.refreshRating(game.getId());

        Events.log(getLocalName(), "reviewed " + game.getTitle() + " with score " + score + "/10" + (suspicious ? " [suspicious]" : ""));

        if (suspicious) {
            ACLMessage flag = new ACLMessage(ACLMessage.INFORM);
            flag.addReceiver(new AID("regulator", AID.ISLOCALNAME));
            flag.setOntology("regulation");
            flag.setContent("REVIEW_FLAG|critic=" + getLocalName() + "|game=" + game.getTitle() + "|score=" + score);
            send(flag);
        }
    }
}

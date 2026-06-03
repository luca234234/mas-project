package com.example.marketplace;

import com.example.marketplace.agents.BuyerAgent;
import com.example.marketplace.agents.CriticAgent;
import com.example.marketplace.agents.DealAggregatorAgent;
import com.example.marketplace.agents.RecommendationAgent;
import com.example.marketplace.agents.RegulatorAgent;
import com.example.marketplace.agents.SellerPublisherAgent;
import com.example.marketplace.db.Database;
import com.example.marketplace.gui.MarketplaceFrame;
import com.example.marketplace.util.Events;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import javax.swing.SwingUtilities;

public class MainApp {
    public static void main(String[] args) {
        Database.init();

        jade.core.Runtime runtime = jade.core.Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "false");
        ContainerController container = runtime.createMainContainer(profile);

        try {
            startAgent(container, "regulator", RegulatorAgent.class);
            startAgent(container, "recommendation", RecommendationAgent.class);
            startAgent(container, "critic", CriticAgent.class);
            startAgent(container, "deal-aggregator", DealAggregatorAgent.class);
            startAgent(container, "seller-alpha", SellerPublisherAgent.class);
            startAgent(container, "seller-beta", SellerPublisherAgent.class);
            startAgent(container, "seller-gamma", SellerPublisherAgent.class);
            AgentController buyer = startAgent(container, "buyer-main", BuyerAgent.class);

            SwingUtilities.invokeLater(() -> new MarketplaceFrame(buyer, container).setVisible(true));
            Events.log("system", "marketplace started");
        } catch (Exception e) {
            throw new IllegalStateException("Could not start JADE platform", e);
        }
    }

    private static AgentController startAgent(ContainerController container, String name, Class<?> agentClass) throws Exception {
        AgentController controller = container.createNewAgent(name, agentClass.getName(), null);
        controller.start();
        return controller;
    }
}

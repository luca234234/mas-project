package com.example.marketplace.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.ArrayList;
import java.util.List;

public final class AgentDirectory {
    private AgentDirectory() {
    }

    public static void register(Agent agent, String serviceType, String serviceName) {
        DFAgentDescription description = new DFAgentDescription();
        description.setName(agent.getAID());

        ServiceDescription service = new ServiceDescription();
        service.setType(serviceType);
        service.setName(serviceName);
        description.addServices(service);

        try {
            DFService.register(agent, description);
        } catch (FIPAException e) {
            throw new IllegalStateException("Could not register service: " + serviceType, e);
        }
    }

    public static List<AID> search(Agent agent, String serviceType) {
        List<AID> results = new ArrayList<>();
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription service = new ServiceDescription();
        service.setType(serviceType);
        template.addServices(service);

        try {
            DFAgentDescription[] matches = DFService.search(agent, template);
            for (DFAgentDescription match : matches) {
                results.add(match.getName());
            }
        } catch (FIPAException e) {
            throw new IllegalStateException("Could not search service: " + serviceType, e);
        }
        return results;
    }
}

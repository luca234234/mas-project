package com.example.marketplace.gui;

import com.example.marketplace.db.Database;
import com.example.marketplace.messaging.MarketCommand;
import com.example.marketplace.model.Deal;
import com.example.marketplace.model.GameOffer;
import com.example.marketplace.util.Events;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Locale;

public class MarketplaceFrame extends JFrame {
    private final AgentController buyerAgent;
    private final ContainerController container;

    private final DefaultTableModel gamesModel = new DefaultTableModel(new Object[]{"Title", "Genre", "Base Price", "Rating", "Lowest Price"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel dealsModel = new DefaultTableModel(new Object[]{"Game", "Seller", "Old Price", "New Price", "Time"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable gamesTable = new JTable(gamesModel);
    private final JTable dealsTable = new JTable(dealsModel);
    private final JTextArea logArea = new JTextArea(14, 80);
    private final JTextField preferencesField = new JTextField("RPG, Strategy, Indie", 22);

    public MarketplaceFrame(AgentController buyerAgent, ContainerController container) {
        super("Multi-Agent Video Game Marketplace");
        this.buyerAgent = buyerAgent;
        this.container = container;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        setSize(980, 680);
        setLocationRelativeTo(null);

        add(topPanel(), BorderLayout.NORTH);
        add(centerPanel(), BorderLayout.CENTER);
        add(logPanel(), BorderLayout.SOUTH);

        Events.onLog(line -> SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
            refreshTables();
        }));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    MarketplaceFrame.this.container.kill();
                } catch (StaleProxyException ignored) {
                }
            }
        });

        refreshTables();
    }

    private JPanel topPanel() {
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshTables());

        JButton buy = new JButton("Buy selected game");
        buy.addActionListener(e -> buySelectedGame());

        JButton recommend = new JButton("Recommendations");
        recommend.addActionListener(e -> requestRecommendations());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panel.add(refresh);
        panel.add(buy);
        panel.add(new JLabel("Preferences:"));
        panel.add(preferencesField);
        panel.add(recommend);
        return panel;
    }

    private JSplitPane centerPanel() {
        JPanel gamesPanel = new JPanel(new BorderLayout());
        gamesPanel.setBorder(BorderFactory.createTitledBorder("Marketplace games"));
        gamesPanel.add(new JScrollPane(gamesTable), BorderLayout.CENTER);

        JPanel dealsPanel = new JPanel(new BorderLayout());
        dealsPanel.setBorder(BorderFactory.createTitledBorder("Recent deals"));
        dealsPanel.add(new JScrollPane(dealsTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gamesPanel, dealsPanel);
        splitPane.setResizeWeight(0.65);
        return splitPane;
    }

    private JPanel logPanel() {
        logArea.setEditable(false);
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setBorder(BorderFactory.createTitledBorder("Agent log"));
        panel.add(new JScrollPane(logArea));
        return panel;
    }

    private void refreshTables() {
        List<GameOffer> offers = Database.gameOffers();
        gamesModel.setRowCount(0);
        for (GameOffer offer : offers) {
            gamesModel.addRow(new Object[]{
                    offer.getTitle(),
                    offer.getGenre(),
                    money(offer.getBasePrice()),
                    offer.getRating(),
                    money(offer.getLowestPrice())
            });
        }

        List<Deal> deals = Database.recentDeals();
        dealsModel.setRowCount(0);
        for (Deal deal : deals) {
            dealsModel.addRow(new Object[]{
                    deal.getGameTitle(),
                    deal.getSeller(),
                    money(deal.getOldPrice()),
                    money(deal.getNewPrice()),
                    deal.getCreatedAt()
            });
        }
    }

    private void buySelectedGame() {
        int row = gamesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a game first.");
            return;
        }

        String title = String.valueOf(gamesModel.getValueAt(row, 0));
        String defaultPrice = String.valueOf(gamesModel.getValueAt(row, 4)).replace("$", "");
        String input = JOptionPane.showInputDialog(this, "Maximum price for " + title + ":", defaultPrice);
        if (input == null || input.isBlank()) {
            return;
        }

        try {
            double maxPrice = Double.parseDouble(input.trim());
            buyerAgent.putO2AObject(MarketCommand.buy(title, maxPrice), false);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a numeric price.");
        } catch (StaleProxyException e) {
            JOptionPane.showMessageDialog(this, "BuyerAgent is not available.");
        }
    }

    private void requestRecommendations() {
        try {
            buyerAgent.putO2AObject(MarketCommand.recommend(preferencesField.getText()), false);
        } catch (StaleProxyException e) {
            JOptionPane.showMessageDialog(this, "BuyerAgent is not available.");
        }
    }

    private String money(double value) {
        return String.format(Locale.US, "$%.2f", value);
    }
}

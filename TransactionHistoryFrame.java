
package spendly;

import com.google.gson.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class TransactionHistoryFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color GREEN = new Color(22, 52, 40);
    private static final Color LIGHT = new Color(249, 249, 247);
    private static final Color MUTED = new Color(114, 121, 116);
    private static final Color INCOME = new Color(40, 116, 71);
    private static final Color EXPENSE = new Color(180, 35, 24);

    private final String token;

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> sortCombo = new JComboBox<>(
        new String[]{"Newest First", "Oldest First",
                     "Highest Amount", "Lowest Amount"}
    );

    private final JLabel incomeValue = new JLabel("₹0.00");
    private final JLabel expenseValue = new JLabel("₹0.00");
    private final JLabel countLabel = new JLabel("Loading transactions...");
    private final JLabel messageLabel = new JLabel("Loading transactions...");

    private final JPanel transactionsPanel = new JPanel();
    private final JPanel messagePanel = new JPanel(new FlowLayout());

    private final JButton allButton = new JButton("All");
    private final JButton expenseButton = new JButton("Expenses");
    private final JButton incomeButton = new JButton("Income");
    private final JButton retryButton = new JButton("Try Again");

    private List<JsonObject> transactions = new ArrayList<>();
    private String activeFilter = "all";

    public TransactionHistoryFrame(String token) {
        this.token = token;

        if (token == null || token.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "Please log in to view transaction history.",
                "Login Required",
                JOptionPane.WARNING_MESSAGE
            );
            dispose();
            return;
        }

        buildUI();
        loadTransactions();
    }

    private void buildUI() {
        setTitle("Spendly - Transaction History");
        setSize(950, 750);
        setMinimumSize(new Dimension(680, 550));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(LIGHT);

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(LIGHT);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel brand = new JLabel("Spendly");
        brand.setFont(new Font("SansSerif", Font.BOLD, 27));
        brand.setForeground(GREEN);

        JLabel subtitle = new JLabel("Transaction History");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);

        header.add(brand);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);

        add(header, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(LIGHT);
        content.setBorder(new EmptyBorder(20, 24, 24, 24));

        // Search, sort and export
        JPanel controls = new JPanel(new BorderLayout(10, 0));
        controls.setBackground(LIGHT);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setToolTipText("Search transactions...");
        searchField.setPreferredSize(new Dimension(220, 38));

        sortCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton exportButton = new JButton("Export CSV");
        styleButton(exportButton, false);
        exportButton.addActionListener(e -> exportCsv());

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightControls.setBackground(LIGHT);
        rightControls.add(sortCombo);
        rightControls.add(exportButton);

        controls.add(searchField, BorderLayout.CENTER);
        controls.add(rightControls, BorderLayout.EAST);

        content.add(controls);
        content.add(Box.createVerticalStrut(16));

        // Filters
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setBackground(LIGHT);
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);

        styleFilterButton(allButton, true);
        styleFilterButton(expenseButton, false);
        styleFilterButton(incomeButton, false);

        allButton.addActionListener(e -> setFilter("all"));
        expenseButton.addActionListener(e -> setFilter("expense"));
        incomeButton.addActionListener(e -> setFilter("income"));

        filters.add(allButton);
        filters.add(expenseButton);
        filters.add(incomeButton);

        content.add(filters);
        content.add(Box.createVerticalStrut(18));

        // Summary cards
        JPanel summary = new JPanel(new GridLayout(1, 2, 12, 0));
        summary.setBackground(LIGHT);
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);

        summary.add(summaryCard("Total Income", incomeValue, INCOME));
        summary.add(summaryCard("Total Expenses", expenseValue, EXPENSE));

        content.add(summary);
        content.add(Box.createVerticalStrut(24));

        JLabel heading = new JLabel("All Transactions");
        heading.setFont(new Font("SansSerif", Font.BOLD, 22));
        heading.setForeground(GREEN);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLabel.setForeground(MUTED);
        countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(heading);
        content.add(Box.createVerticalStrut(5));
        content.add(countLabel);
        content.add(Box.createVerticalStrut(12));

        // Loading and error message
        messagePanel.setBackground(LIGHT);
        messageLabel.setForeground(MUTED);
        retryButton.addActionListener(e -> loadTransactions());

        messagePanel.add(messageLabel);
        messagePanel.add(retryButton);
        messagePanel.setVisible(false);
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(messagePanel);

        // Transaction list
        transactionsPanel.setLayout(
            new BoxLayout(transactionsPanel, BoxLayout.Y_AXIS)
        );
        transactionsPanel.setBackground(LIGHT);
        transactionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(transactionsPanel);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(LIGHT);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        // Bottom navigation
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        nav.setBackground(Color.WHITE);
        nav.setBorder(BorderFactory.createMatteBorder(
            1, 0, 0, 0, new Color(232, 232, 230)
        ));

        addNavButton(nav, "Home", "DashboardFrame");
        addNavButton(nav, "History", "TransactionHistoryFrame");

        JButton addButton = new JButton("+ Add");
        styleButton(addButton, true);
        addButton.addActionListener(e -> openPage("AddTransactionFrame"));
        nav.add(addButton);

        addNavButton(nav, "Insights", "AnalyticsFrame");
        addNavButton(nav, "Budgets", "BudgetsFrame");

        add(nav, BorderLayout.SOUTH);

        // Search as user types
        searchField.getDocument().addDocumentListener(
            new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    renderTransactions();
                }

                public void removeUpdate(DocumentEvent e) {
                    renderTransactions();
                }

                public void changedUpdate(DocumentEvent e) {
                    renderTransactions();
                }
            }
        );

        sortCombo.addActionListener(e -> renderTransactions());
    }

    private JPanel summaryCard(
        String title, JLabel value, Color valueColor
    ) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(240, 240, 238)),
            new EmptyBorder(15, 17, 15, 17)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        value.setForeground(valueColor);
        value.setFont(new Font("SansSerif", Font.BOLD, 22));

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(9));
        card.add(value);

        return card;
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));

        if (primary) {
            button.setBackground(GREEN);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(new Color(232, 232, 230));
            button.setForeground(GREEN);
        }
    }

    private void styleFilterButton(JButton button, boolean active) {
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setBorder(new EmptyBorder(9, 16, 9, 16));
        button.setBackground(active ? GREEN : new Color(232, 232, 230));
        button.setForeground(active ? Color.WHITE : GREEN);
    }

    private void setFilter(String filter) {
        activeFilter = filter;

        styleFilterButton(allButton, "all".equals(filter));
        styleFilterButton(expenseButton, "expense".equals(filter));
        styleFilterButton(incomeButton, "income".equals(filter));

        renderTransactions();
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    private String getTitle(JsonObject t) {
        String title = getString(t, "title");

        if (title.isEmpty()) {
            title = getString(t, "description");
        }

        return title.isEmpty() ? "Transaction" : title;
    }

    private String getCategory(JsonObject t) {
        String category = getString(t, "category");
        return category.isEmpty() ? "Other" : category;
    }

    private double getAmount(JsonObject t) {
        try {
            JsonElement value = t.get("amount");

            if (value != null && !value.isJsonNull()) {
                return value.getAsDouble();
            }
        } catch (Exception ignored) {
            // Missing or invalid amount.
        }

        return 0;
    }

    private String getString(JsonObject object, String key) {
        try {
            JsonElement value = object.get(key);

            if (value != null && !value.isJsonNull()) {
                return value.getAsString();
            }
        } catch (Exception ignored) {
            // Missing or invalid string.
        }

        return "";
    }

    private long getId(JsonObject t) {
        try {
            JsonElement value = t.get("id");

            if (value != null && !value.isJsonNull()) {
                return value.getAsLong();
            }
        } catch (Exception ignored) {
            // Missing ID.
        }

        return 0L;
    }

    private String formatMoney(double amount) {
        return "₹" + String.format(Locale.US, "%,.2f", amount);
    }

    // Fetch transactions from the Java backend
    private void loadTransactions() {
        retryButton.setEnabled(false);
        messageLabel.setText("Loading your transactions...");
        messagePanel.setVisible(true);

        SwingWorker<List<JsonObject>, Void> worker =
            new SwingWorker<List<JsonObject>, Void>() {

            @Override
            protected List<JsonObject> doInBackground() throws Exception {
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(
                        "http://localhost:8080/transactions"
                    );

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty(
                        "Authorization", "Bearer " + token
                    );
                    connection.setRequestProperty(
                        "Accept", "application/json"
                    );
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    int status = connection.getResponseCode();

                    if (status == 401 || status == 403) {
                        throw new IOException(
                            "Login token invalid or expired. Please log in again."
                        );
                    }

                    if (status < 200 || status >= 300) {
                        throw new IOException(
                            "Server returned HTTP " + status
                        );
                    }

                    StringBuilder body = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                            connection.getInputStream(),
                            StandardCharsets.UTF_8
                        )
                    )) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            body.append(line);
                        }
                    }

                    // Compatible with older Gson versions
                    JsonElement data = new JsonParser().parse(body.toString());
                    JsonArray array = new JsonArray();

                    if (data.isJsonArray()) {
                        array = data.getAsJsonArray();
                    } else if (data.isJsonObject()
                        && data.getAsJsonObject().has("transactions")
                        && data.getAsJsonObject()
                            .get("transactions").isJsonArray()) {

                        array = data.getAsJsonObject()
                            .getAsJsonArray("transactions");
                    }

                    List<JsonObject> result = new ArrayList<>();

                    for (JsonElement element : array) {
                        if (element.isJsonObject()) {
                            result.add(element.getAsJsonObject());
                        }
                    }

                    return result;

                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            @Override
            protected void done() {
                retryButton.setEnabled(true);

                try {
                    transactions = get();
                    updateSummary();
                    renderTransactions();
                    messagePanel.setVisible(false);

                } catch (Exception ex) {
                    ex.printStackTrace();

                    messageLabel.setText(
                        "Unable to load transactions. Check server and login."
                    );
                    messagePanel.setVisible(true);
                    countLabel.setText("");
                }
            }
        };

        worker.execute();
    }

    private void updateSummary() {
        double totalIncome = 0;
        double totalExpenses = 0;

        for (JsonObject t : transactions) {
            String type = normalizeType(getString(t, "type"));
            double amount = getAmount(t);

            if ("income".equals(type)) {
                totalIncome += amount;
            } else if ("expense".equals(type)) {
                totalExpenses += amount;
            }
        }

        incomeValue.setText(formatMoney(totalIncome));
        expenseValue.setText(formatMoney(totalExpenses));
    }

    private void renderTransactions() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> renderTransactions());
            return;
        }

        String search = searchField.getText()
            .toLowerCase(Locale.ROOT).trim();

        List<JsonObject> filtered = new ArrayList<>();

        for (JsonObject t : transactions) {
            String type = normalizeType(getString(t, "type"));

            boolean matchesType =
                "all".equals(activeFilter) || activeFilter.equals(type);

            String searchable = (
                getTitle(t) + " " + getCategory(t) + " " + getAmount(t)
            ).toLowerCase(Locale.ROOT);

            if (matchesType && searchable.contains(search)) {
                filtered.add(t);
            }
        }

        String sort = (String) sortCombo.getSelectedItem();

        if ("Highest Amount".equals(sort)) {
            filtered.sort((a, b) ->
                Double.compare(getAmount(b), getAmount(a))
            );
        } else if ("Lowest Amount".equals(sort)) {
            filtered.sort((a, b) ->
                Double.compare(getAmount(a), getAmount(b))
            );
        } else if ("Oldest First".equals(sort)) {
            filtered.sort((a, b) ->
                Long.compare(getId(a), getId(b))
            );
        } else {
            filtered.sort((a, b) ->
                Long.compare(getId(b), getId(a))
            );
        }

        transactionsPanel.removeAll();
        countLabel.setText(filtered.size() + " transaction(s) found");

        if (filtered.isEmpty()) {
            JLabel empty = new JLabel(
                transactions.isEmpty()
                    ? "You haven't added any transactions yet."
                    : "No transactions match your search."
            );

            empty.setForeground(MUTED);
            empty.setBorder(new EmptyBorder(24, 10, 24, 10));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);

            transactionsPanel.add(empty);

        } else {
            for (JsonObject t : filtered) {
                transactionsPanel.add(createTransactionCard(t));
                transactionsPanel.add(Box.createVerticalStrut(10));
            }
        }

        transactionsPanel.revalidate();
        transactionsPanel.repaint();
    }

    private JPanel createTransactionCard(JsonObject t) {
        JPanel card = new JPanel(new BorderLayout(12, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(240, 240, 238)),
            new EmptyBorder(15, 16, 15, 16)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean isIncome =
            "income".equals(normalizeType(getString(t, "type")));

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBackground(Color.WHITE);

        JLabel title = new JLabel(getTitle(t));
        title.setForeground(GREEN);
        title.setFont(new Font("SansSerif", Font.BOLD, 15));

        JLabel category = new JLabel(
            getCategory(t) + " • " + (isIncome ? "Income" : "Expense")
        );
        category.setForeground(MUTED);
        category.setFont(new Font("SansSerif", Font.PLAIN, 12));

        details.add(title);
        details.add(Box.createVerticalStrut(6));
        details.add(category);

        JLabel amount = new JLabel(
            (isIncome ? "+" : "-") + formatMoney(getAmount(t))
        );
        amount.setFont(new Font("SansSerif", Font.BOLD, 14));
        amount.setForeground(isIncome ? INCOME : EXPENSE);

        card.add(details, BorderLayout.CENTER);
        card.add(amount, BorderLayout.EAST);

        return card;
    }

    // Export every transaction to a CSV file
    private void exportCsv() {
        if (transactions.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "There are no transactions to export.",
                "Export CSV",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save transaction history as CSV");
        chooser.setSelectedFile(new java.io.File("spendly_transactions.csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(chooser.getSelectedFile()),
                StandardCharsets.UTF_8
            )
        )) {
            writer.write('\uFEFF');
            writer.write("Title,Category,Type,Amount");
            writer.newLine();

            for (JsonObject t : transactions) {
                writeCsvField(writer, getTitle(t));
                writer.write(",");
                writeCsvField(writer, getCategory(t));
                writer.write(",");
                writeCsvField(writer, getString(t, "type"));
                writer.write(",");
                writeCsvField(writer, String.valueOf(getAmount(t)));
                writer.newLine();
            }

            JOptionPane.showMessageDialog(
                this,
                "CSV exported successfully.",
                "Export CSV",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not export CSV:\n" + ex.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void writeCsvField(BufferedWriter writer, String value)
        throws IOException {

        String safe = value == null ? "" : value;
        writer.write("\"");
        writer.write(safe.replace("\"", "\"\""));
        writer.write("\"");
    }

    private void addNavButton(JPanel nav, String label, String page) {
        JButton button = new JButton(label);
        styleButton(button, false);

        button.addActionListener(e -> {
            if ("TransactionHistoryFrame".equals(page)) {
                loadTransactions();
            } else {
                openPage(page);
            }
        });

        nav.add(button);
    }

    private void openPage(String page) {
        try {
            Class<?> clazz = Class.forName("spendly." + page);
            Object object;

            try {
                object = clazz.getConstructor(String.class).newInstance(token);
            } catch (NoSuchMethodException ex) {
                object = clazz.getConstructor().newInstance();
            }

            if (object instanceof JFrame) {
                ((JFrame) object).setVisible(true);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not open " + page + ".\n" + ex.getMessage(),
                "Navigation Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String token = JOptionPane.showInputDialog(
                null, "Enter your login token:"
            );

            if (token != null && !token.trim().isEmpty()) {
                new TransactionHistoryFrame(token).setVisible(true);
            }
        });
    }
}

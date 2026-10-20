
package spendly;

import com.google.gson.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color GREEN = new Color(22, 52, 40);
    private static final Color LIGHT = new Color(249, 249, 247);
    private static final Color MUTED = new Color(114, 121, 116);

    private static final Color[] COLORS = {
        new Color(121, 44, 23),
        new Color(45, 75, 62),
        new Color(67, 102, 77),
        new Color(173, 206, 189),
        new Color(193, 200, 195),
        new Color(91, 22, 4),
        new Color(22, 52, 40),
        new Color(114, 121, 116),
        new Color(153, 186, 169),
        new Color(176, 137, 104)
    };

    private final String token;

    private final String[] categories = {
        "Food", "Transportation", "Shopping", "Education",
        "Fun", "Bills", "Health", "Care", "Subs", "Other"
    };

    private final JLabel spentValue = new JLabel("₹0");
    private final JLabel earnedValue = new JLabel("₹0");
    private final JLabel averageValue = new JLabel("₹0");
    private final JLabel balanceValue = new JLabel("₹0");

    private final JLabel dateLabel = new JLabel();
    private final JLabel donutTotal = new JLabel("₹0");

    private final JLabel insightLabel = new JLabel(
        "<html>Add transactions to see your financial insights.</html>"
    );

    private final JLabel errorLabel = new JLabel(" ");

    private final JButton retryButton = new JButton("Try Again");

    private final JPanel categoryPanel = new JPanel();
    private final JPanel recentPanel = new JPanel();
    private final JPanel errorPanel = new JPanel(
        new FlowLayout(FlowLayout.LEFT)
    );

    private final DonutChart donutChart = new DonutChart();

    private final Map<String, Double> categoryTotals =
        new LinkedHashMap<String, Double>();

    public AnalyticsFrame(String token) {
        this.token = token;

        if (token == null || token.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "Please log in to view Analytics.",
                "Login Required",
                JOptionPane.WARNING_MESSAGE
            );
            dispose();
            return;
        }

        for (String category : categories) {
            categoryTotals.put(category, 0.0);
        }

        buildUI();
        loadAnalytics();
    }

    private void buildUI() {
        setTitle("Spendly - Analytics");
        setSize(1000, 800);
        setMinimumSize(new Dimension(700, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(LIGHT);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(LIGHT);

        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                0, 0, 1, 0, new Color(232, 232, 230)
            ),
            new EmptyBorder(16, 22, 14, 22)
        ));

        JLabel brand = new JLabel("Spendly");
        brand.setFont(new Font("SansSerif", Font.BOLD, 24));
        brand.setForeground(GREEN);

        JLabel subtitle = new JLabel(
            "Your personal finance companion"
        );
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

        header.add(brand);
        header.add(Box.createVerticalStrut(3));
        header.add(subtitle);

        add(header, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(LIGHT);
        content.setBorder(new EmptyBorder(20, 24, 25, 24));

        // Page heading
        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(LIGHT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JPanel headingLeft = new JPanel();
        headingLeft.setLayout(
            new BoxLayout(headingLeft, BoxLayout.Y_AXIS)
        );
        headingLeft.setBackground(LIGHT);

        JLabel eyebrow = new JLabel("PERFORMANCE SNAPSHOT");
        eyebrow.setForeground(new Color(67, 102, 77));
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));

        JLabel title = new JLabel("Financial Insights");
        title.setForeground(GREEN);
        title.setFont(new Font("SansSerif", Font.BOLD, 27));

        headingLeft.add(eyebrow);
        headingLeft.add(Box.createVerticalStrut(5));
        headingLeft.add(title);

        dateLabel.setForeground(new Color(67, 102, 77));
        dateLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        updateDateLabel();

        heading.add(headingLeft, BorderLayout.WEST);
        heading.add(dateLabel, BorderLayout.EAST);

        content.add(heading);
        content.add(Box.createVerticalStrut(18));

        // Error and retry panel
        errorLabel.setForeground(new Color(147, 0, 10));
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        retryButton.addActionListener(e -> loadAnalytics());

        errorPanel.setBackground(new Color(255, 218, 214));
        errorPanel.add(errorLabel);
        errorPanel.add(retryButton);
        errorPanel.setVisible(false);

        errorPanel.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 55)
        );

        content.add(errorPanel);
        content.add(Box.createVerticalStrut(10));

        // Summary cards
        JPanel metrics = new JPanel(new GridLayout(2, 2, 12, 12));
        metrics.setBackground(LIGHT);
        metrics.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 220)
        );

        metrics.add(metricCard(
            "Total Spent",
            spentValue,
            "Your recorded expenses"
        ));

        metrics.add(metricCard(
            "Total Earned",
            earnedValue,
            "Your recorded income"
        ));

        metrics.add(metricCard(
            "Daily Average",
            averageValue,
            "Average expenses per day this month"
        ));

        metrics.add(metricCard(
            "Remaining Balance",
            balanceValue,
            "Income minus expenses"
        ));

        content.add(metrics);
        content.add(Box.createVerticalStrut(18));

        // Category distribution
        JPanel distribution = cardPanel();
        distribution.setLayout(new BorderLayout(8, 12));

        distribution.add(
            sectionTitle("Category Distribution"),
            BorderLayout.NORTH
        );

        JPanel distributionBody =
            new JPanel(new GridLayout(1, 2, 15, 10));

        distributionBody.setBackground(Color.WHITE);

        JPanel chartSide = new JPanel(new BorderLayout());
        chartSide.setBackground(Color.WHITE);

        donutTotal.setHorizontalAlignment(SwingConstants.CENTER);
        donutTotal.setFont(new Font("SansSerif", Font.BOLD, 20));
        donutTotal.setForeground(GREEN);

        chartSide.add(donutChart, BorderLayout.CENTER);
        chartSide.add(donutTotal, BorderLayout.SOUTH);

        categoryPanel.setLayout(
            new BoxLayout(categoryPanel, BoxLayout.Y_AXIS)
        );
        categoryPanel.setBackground(Color.WHITE);

        JScrollPane categoryScroll =
            new JScrollPane(categoryPanel);

        categoryScroll.setBorder(null);
        categoryScroll.getViewport().setBackground(Color.WHITE);
        categoryScroll.setPreferredSize(new Dimension(320, 270));

        distributionBody.add(chartSide);
        distributionBody.add(categoryScroll);

        distribution.add(
            distributionBody,
            BorderLayout.CENTER
        );

        content.add(distribution);
        content.add(Box.createVerticalStrut(18));

        // Spending insight
        JPanel insightCard = new JPanel(
            new BorderLayout(8, 10)
        );

        insightCard.setBackground(new Color(228, 240, 231));
        insightCard.setBorder(
            new EmptyBorder(18, 18, 18, 18)
        );

        insightCard.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 140)
        );

        JLabel insightTitle = new JLabel("Spending Insight");
        insightTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        insightTitle.setForeground(GREEN);

        insightLabel.setFont(
            new Font("SansSerif", Font.PLAIN, 13)
        );
        insightLabel.setForeground(new Color(66, 72, 68));

        insightCard.add(insightTitle, BorderLayout.NORTH);
        insightCard.add(insightLabel, BorderLayout.CENTER);

        content.add(insightCard);
        content.add(Box.createVerticalStrut(18));

        // Recent transactions
        JPanel recentCard = cardPanel();
        recentCard.setLayout(new BorderLayout(8, 10));

        recentCard.add(
            sectionTitle("Recent Transactions"),
            BorderLayout.NORTH
        );

        recentPanel.setLayout(
            new BoxLayout(recentPanel, BoxLayout.Y_AXIS)
        );
        recentPanel.setBackground(Color.WHITE);

        JScrollPane recentScroll = new JScrollPane(recentPanel);
        recentScroll.setBorder(null);
        recentScroll.getViewport().setBackground(Color.WHITE);
        recentScroll.setPreferredSize(new Dimension(600, 230));

        recentCard.add(recentScroll, BorderLayout.CENTER);

        content.add(recentCard);

        // Main scroll pane
        JScrollPane mainScroll = new JScrollPane(content);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setBackground(LIGHT);

        add(mainScroll, BorderLayout.CENTER);

        // Bottom navigation
        JPanel nav = new JPanel(
            new FlowLayout(FlowLayout.CENTER, 18, 12)
        );

        nav.setBackground(LIGHT);
        nav.setBorder(
            BorderFactory.createMatteBorder(
                1, 0, 0, 0, new Color(232, 232, 230)
            )
        );

        addNavButton(nav, "Home", "DashboardFrame");
        addNavButton(nav, "History", "TransactionHistoryFrame");

        JButton addButton = new JButton("+ Add");
        addButton.setBackground(GREEN);
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);

        addButton.addActionListener(
            e -> openPage("AddTransactionFrame")
        );

        nav.add(addButton);

        addNavButton(nav, "Insights", "AnalyticsFrame");
        addNavButton(nav, "Budgets", "BudgetsFrame");

        add(nav, BorderLayout.SOUTH);
    }

    private void updateDateLabel() {
        dateLabel.setText(
            LocalDate.now().format(
                DateTimeFormatter.ofPattern(
                    "MMM yyyy",
                    Locale.ENGLISH
                )
            )
        );
    }

    private JPanel metricCard(
        String label,
        JLabel value,
        String note
    ) {
        JPanel panel = new JPanel();

        panel.setLayout(
            new BoxLayout(panel, BoxLayout.Y_AXIS)
        );

        panel.setBackground(Color.WHITE);

        panel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(240, 240, 238)
                ),
                new EmptyBorder(16, 17, 16, 17)
            )
        );

        JLabel name = new JLabel(label);
        name.setForeground(MUTED);
        name.setFont(new Font("SansSerif", Font.PLAIN, 12));

        value.setForeground(GREEN);
        value.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel description = new JLabel(note);
      description.setForeground(new Color(67, 102, 77));
        description.setFont(new Font("SansSerif", Font.PLAIN, 11));

        panel.add(name);
        panel.add(Box.createVerticalStrut(8));
        panel.add(value);
        panel.add(Box.createVerticalStrut(6));
        panel.add(description);

        return panel;
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();

        panel.setBackground(Color.WHITE);

        panel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(240, 240, 238)
                ),
                new EmptyBorder(18, 18, 18, 18)
            )
        );

        panel.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 410)
        );

        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(GREEN);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        return label;
    }

    private void addNavButton(
        JPanel nav,
        String label,
        String page
    ) {
        JButton button = new JButton(label);
        button.setFocusPainted(false);

        button.addActionListener(e -> {
            if ("AnalyticsFrame".equals(page)) {
                loadAnalytics();
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
                object = clazz
                    .getConstructor(String.class)
                    .newInstance(token);

            } catch (NoSuchMethodException ex) {
                object = clazz
                    .getConstructor()
                    .newInstance();
            }

            if (object instanceof JFrame) {
                ((JFrame) object).setVisible(true);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not open " + page + ".\n"
                    + ex.getMessage(),
                "Navigation Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String money(double amount) {
        return "₹" + String.format(
            Locale.US,
            "%,.2f",
            amount
        ).replaceAll("\\.00$", "");
    }

    private String normalizeCategory(String value) {
        if (value != null) {
            for (String category : categories) {
                if (category.equalsIgnoreCase(value.trim())) {
                    return category;
                }
            }
        }

        return "Other";
    }

    private String normalizeType(String value) {
        return value == null
            ? ""
            : value.trim().toLowerCase(Locale.ROOT);
    }

    // Load transactions using HttpURLConnection (Java 8 compatible)
    private void loadAnalytics() {
        retryButton.setEnabled(false);
        errorLabel.setText("Loading transactions...");
        errorPanel.setVisible(true);

        SwingWorker<JsonArray, Void> worker =
            new SwingWorker<JsonArray, Void>() {

            @Override
            protected JsonArray doInBackground() throws Exception {
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(
                        "http://localhost:8080/transactions"
                    );

                    connection =
                        (HttpURLConnection) url.openConnection();

                    connection.setRequestMethod("GET");

                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + token
                    );

                    connection.setRequestProperty(
                        "Accept",
                        "application/json"
                    );

                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    int status = connection.getResponseCode();

                    if (status < 200 || status >= 300) {
                        String message =
                            "Server returned HTTP " + status;

                        throw new IOException(message);
                    }

                    StringBuilder responseText =
                        new StringBuilder();

                    try (
                        BufferedReader reader =
                            new BufferedReader(
                                new InputStreamReader(
                                    connection.getInputStream(),
                                    StandardCharsets.UTF_8
                                )
                            )
                    ) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            responseText.append(line);
                        }
                    }

                    JsonElement json =
                        JsonParser.parseString(
                            responseText.toString()
                        );

                    if (json.isJsonArray()) {
                        return json.getAsJsonArray();
                    }

                    if (json.isJsonObject()) {
                        JsonObject responseObject =
                            json.getAsJsonObject();

                        if (responseObject.has("transactions")
                            && responseObject
                                .get("transactions").isJsonArray()) {

                            return responseObject
                                .getAsJsonArray("transactions");
                        }
                    }

                    return new JsonArray();

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
                    JsonArray transactions = get();

                    processTransactions(transactions);

                    errorLabel.setText(" ");
                    errorPanel.setVisible(false);

                } catch (Exception ex) {
                    ex.printStackTrace();

                    errorLabel.setText(
                        "Unable to load transactions. "
                        + "Check server and login."
                    );

                    errorPanel.setVisible(true);
                }

                errorPanel.revalidate();
                errorPanel.repaint();
            }
        };

        worker.execute();
    }

    private void processTransactions(JsonArray transactions) {
        double totalIncome = 0;
        double totalExpenses = 0;

        for (String category : categories) {
            categoryTotals.put(category, 0.0);
        }

        List<JsonObject> transactionList =
            new ArrayList<JsonObject>();

        for (JsonElement element : transactions) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject transaction = element.getAsJsonObject();
            transactionList.add(transaction);

            double amount = getNumber(transaction, "amount");
            String type = getString(transaction, "type");

            if ("income".equals(normalizeType(type))) {
                totalIncome += amount;

            } else if ("expense".equals(normalizeType(type))) {
                totalExpenses += amount;

                String category = normalizeCategory(
                    getString(transaction, "category")
                );

                categoryTotals.put(
                    category,
                    categoryTotals.get(category) + amount
                );
            }
        }

        spentValue.setText(money(totalExpenses));
        earnedValue.setText(money(totalIncome));
        balanceValue.setText(
            money(totalIncome - totalExpenses)
        );

        int dayOfMonth = LocalDate.now().getDayOfMonth();

        averageValue.setText(
            money(
                dayOfMonth == 0
                    ? 0
                    : totalExpenses / dayOfMonth
            )
        );

        updateDateLabel();

        displayCategories();
        donutChart.repaint();

        donutTotal.setText(money(totalExpenses));

        updateInsight(totalExpenses, totalIncome);
        displayRecent(transactionList);
    }

    private double getNumber(
        JsonObject object,
        String key
    ) {
        try {
            JsonElement value = object.get(key);

            if (value != null && !value.isJsonNull()) {
                return value.getAsDouble();
            }

        } catch (Exception ignored) {
            // Return zero for missing or invalid values.
        }

        return 0;
    }

    private String getString(
        JsonObject object,
        String key
    ) {
        try {
            JsonElement value = object.get(key);

            if (value != null && !value.isJsonNull()) {
                return value.getAsString();
            }

        } catch (Exception ignored) {
            // Return an empty string for missing or invalid values.
        }

        return "";
    }

    private void displayCategories() {
        categoryPanel.removeAll();

        double total = 0;

        for (Double amount : categoryTotals.values()) {
            total += amount;
        }

        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];

            double amount = categoryTotals.get(category);

            double percent = total > 0
                ? amount / total * 100
                : 0;

            JPanel item = new JPanel();

            item.setLayout(
                new BoxLayout(item, BoxLayout.Y_AXIS)
            );

            item.setBackground(new Color(244, 244, 242));
            item.setBorder(
                new EmptyBorder(10, 12, 10, 12)
            );

            item.setAlignmentX(Component.LEFT_ALIGNMENT);

            item.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 90)
            );

            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);

            JLabel name = new JLabel(category);
            name.setForeground(GREEN);
            name.setFont(new Font("SansSerif", Font.BOLD, 13));

            JLabel amountLabel = new JLabel(money(amount));
            amountLabel.setForeground(GREEN);
            amountLabel.setFont(
                new Font("SansSerif", Font.BOLD, 13)
            );

            row.add(name, BorderLayout.WEST);
            row.add(amountLabel, BorderLayout.EAST);

            JLabel percentLabel = new JLabel(
                String.format(
                    Locale.US,
                    "%.1f%% of total expenses",
                    percent
                )
            );

            percentLabel.setForeground(MUTED);
            percentLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 10)
            );

            JProgressBar progress =
                new JProgressBar(0, 100);

            progress.setValue(
                (int) Math.round(percent)
            );

            progress.setStringPainted(false);
            progress.setForeground(COLORS[i]);
            progress.setBackground(
                new Color(226, 227, 225)
            );
            progress.setBorderPainted(false);

            progress.setPreferredSize(
                new Dimension(200, 7)
            );

            progress.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 7)
            );

            item.add(row);
            item.add(Box.createVerticalStrut(5));
            item.add(percentLabel);
            item.add(Box.createVerticalStrut(5));
            item.add(progress);

            categoryPanel.add(item);
            categoryPanel.add(Box.createVerticalStrut(7));
        }

        categoryPanel.revalidate();
        categoryPanel.repaint();
    }

    private void updateInsight(
        double expenses,
        double income
    ) {
        if (expenses == 0 && income == 0) {
            insightLabel.setText(
                "<html>Add your income and expenses "
                + "to see your financial insights.</html>"
            );
            return;
        }

        String highestCategory = "Other";
        double highestAmount = 0;

        for (String category : categories) {
            double amount = categoryTotals.get(category);

            if (amount > highestAmount) {
                highestAmount = amount;
                highestCategory = category;
            }
        }

        String message;

        if (expenses > 0) {
            message =
                "Your highest spending category is "
                + highestCategory
                + ", with "
                + money(highestAmount)
                + " in expenses. ";

        } else {
            message =
                "You have not recorded any expenses yet. ";
        }

        double balance = income - expenses;

        if (balance > 0) {
            message +=
                "Your recorded income exceeds expenses by "
                + money(balance) + ".";

        } else if (balance < 0) {
            message +=
                "Your expenses exceed recorded income by "
                + money(Math.abs(balance)) + ".";

        } else {
            message +=
                "Your recorded income and expenses are equal.";
        }

        insightLabel.setText(
            "<html>" + message + "</html>"
        );
    }

    private void displayRecent(
        List<JsonObject> transactions
    ) {
        recentPanel.removeAll();

        int start = Math.max(
            0,
            transactions.size() - 5
        );

        for (
            int i = transactions.size() - 1;
            i >= start;
            i--
        ) {
            JsonObject transaction = transactions.get(i);

            String type = normalizeType(
                getString(transaction, "type")
            );

            boolean income = "income".equals(type);

            String title = getString(
                transaction,
                "title"
            );

            if (title.isEmpty()) {
                title = "Transaction";
            }

            String category = normalizeCategory(
                getString(transaction, "category")
            );

            double amount = getNumber(
                transaction,
                "amount"
            );

            JPanel row = new JPanel(
                new BorderLayout(12, 5)
            );

            row.setBackground(Color.WHITE);
            row.setBorder(
                new EmptyBorder(12, 2, 12, 2)
            );

            row.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 65)
            );

            JPanel details = new JPanel();

            details.setLayout(
                new BoxLayout(details, BoxLayout.Y_AXIS)
            );

            details.setBackground(Color.WHITE);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(GREEN);
            titleLabel.setFont(
                new Font("SansSerif", Font.BOLD, 13)
            );

            JLabel categoryLabel = new JLabel(category);
            categoryLabel.setForeground(MUTED);
            categoryLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 11)
            );

            details.add(titleLabel);
            details.add(Box.createVerticalStrut(4));
            details.add(categoryLabel);

            JLabel amountLabel = new JLabel(
                (income ? "+" : "-") + money(amount)
            );

            amountLabel.setForeground(
                income
                    ? new Color(40, 116, 71)
                    : new Color(121, 44, 23)
            );

            amountLabel.setFont(
                new Font("SansSerif", Font.BOLD, 13)
            );

            row.add(details, BorderLayout.CENTER);
            row.add(amountLabel, BorderLayout.EAST);

            recentPanel.add(row);
            recentPanel.add(new JSeparator());
        }

        if (transactions.isEmpty()) {
            recentPanel.add(
                new JLabel("No transactions recorded yet.")
            );
        }

        recentPanel.revalidate();
        recentPanel.repaint();
    }

    // Donut chart drawn directly using Swing
    private class DonutChart extends JPanel {

        private static final long serialVersionUID = 1L;

        DonutChart() {
            setBackground(Color.WHITE);
            setPreferredSize(
                new Dimension(260, 260)
            );
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int size = Math.min(
                getWidth(),
                getHeight()
            ) - 35;

            if (size <= 0) {
                g2.dispose();
                return;
            }

            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            double total = 0;

            for (Double amount : categoryTotals.values()) {
                total += amount;
            }

            g2.setStroke(
                new BasicStroke(
                    22,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_ROUND
                )
            );

            g2.setColor(
                new Color(244, 244, 242)
            );

            g2.drawOval(x, y, size, size);

            if (total > 0) {
                double startAngle = 90;

                for (int i = 0; i < categories.length; i++) {
                    double amount =
                        categoryTotals.get(categories[i]);

                    if (amount <= 0) {
                        continue;
                    }

                    double angle =
                        amount / total * 360.0;

                    g2.setColor(COLORS[i]);

                    g2.draw(
                        new java.awt.geom.Arc2D.Double(
                            x,
                            y,
                            size,
                            size,
                            startAngle,
                            -angle,
                            java.awt.geom.Arc2D.OPEN
                        )
                    );

                    startAngle -= angle;
                }
            }

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String token = JOptionPane.showInputDialog(
                null,
                "Enter your login token:"
            );

            if (token != null && !token.trim().isEmpty()) {
                new AnalyticsFrame(token).setVisible(true);
            }
        });
    }
}

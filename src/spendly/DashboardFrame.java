
package spendly;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.regex.*;

public class DashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color GREEN = new Color(22, 52, 40);
    private static final Color HOVER = new Color(36, 91, 70);
    private static final Color BG = new Color(249, 249, 247);
    private static final Color RED = new Color(192, 57, 43);
    private static final Color INCOME_GREEN = new Color(22, 128, 61);
    private static final String API_URL = "http://localhost:8080";

    private String token;

    private JLabel balanceLabel;
    private JLabel incomeLabel;
    private JLabel expenseLabel;
    private JLabel savedLabel;

    private JPanel transactionsPanel;

    private final DecimalFormat money =
            new DecimalFormat("#,##0.00");

    public DashboardFrame(String token) {
        this.token = token;

        setTitle("Spendly - Dashboard");
        setSize(1000, 750);
        setMinimumSize(new Dimension(650, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);
        add(createDashboard(), BorderLayout.CENTER);
        add(createNavigation(), BorderLayout.SOUTH);

        if (token != null && !token.trim().isEmpty()) {
            loadDashboard();
        } else {
            setValues("Login required", "--", "--", "--");
            showTransactionMessage(
                    "Please log in to view your transactions.");
        }
    }

    // HEADER
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel logo = new JLabel("Spendly");
        logo.setFont(new Font("Arial", Font.BOLD, 30));
        logo.setForeground(GREEN);

        JPanel right = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(BG);

        JButton profile = createButton("Profile");
        JButton logout = createButton("Logout");

        // UPDATED: Open ProfileFrame directly with login token.
        profile.addActionListener(e -> openProfile());

        logout.addActionListener(e -> logout());

        right.add(profile);
        right.add(logout);

        header.add(logo, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // OPEN PROFILE SCREEN
    private void openProfile() {
        if (token == null || token.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please log in before opening your profile.",
                    "Login Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ProfileFrame frame = new ProfileFrame(token);
            frame.setVisible(true);
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Could not open Profile page:\n" + ex.getMessage(),
                    "Profile Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // DASHBOARD CONTENT
    private JScrollPane createDashboard() {
        JPanel content = new JPanel();
        content.setLayout(
                new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(0, 24, 24, 24));

        content.add(createBalanceCard());
        content.add(Box.createVerticalStrut(15));
        content.add(createActionButtons());
        content.add(Box.createVerticalStrut(15));
        content.add(createBudgetCard());
        content.add(Box.createVerticalStrut(15));
        content.add(createTransactionsCard());
        content.add(Box.createVerticalStrut(15));
        content.add(createCategoryCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    // AVAILABLE BALANCE CARD
    private JPanel createBalanceCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(GREEN);
        card.setBorder(new EmptyBorder(24, 24, 24, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Available Balance");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.PLAIN, 16));

        balanceLabel = new JLabel("₹0.00");
        balanceLabel.setForeground(Color.WHITE);
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 32));
        balanceLabel.setBorder(new EmptyBorder(12, 0, 18, 0));

        JPanel summary = new JPanel(new GridLayout(1, 3, 10, 0));
        summary.setBackground(GREEN);

        incomeLabel = new JLabel("₹0.00");
        expenseLabel = new JLabel("₹0.00");
        savedLabel = new JLabel("₹0.00");

        summary.add(createSummary("Income", incomeLabel));
        summary.add(createSummary("Spent", expenseLabel));
        summary.add(createSummary("Saved", savedLabel));

        card.add(title);
        card.add(balanceLabel);
        card.add(summary);

        return card;
    }

    private JPanel createSummary(String title, JLabel value) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(GREEN);

        JLabel heading = new JLabel(title);
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("Arial", Font.PLAIN, 14));

        value.setForeground(Color.WHITE);
        value.setFont(new Font("Arial", Font.BOLD, 15));

        panel.add(heading);
        panel.add(Box.createVerticalStrut(7));
        panel.add(value);

        return panel;
    }

    // QUICK ACTION BUTTONS
    private JPanel createActionButtons() {
        JPanel panel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBackground(BG);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton quickAdd = createButton("+ Quick Add");
        JButton setBudget = createButton("Set Budget");
        JButton report = createButton("Monthly Report");

        quickAdd.addActionListener(e -> openAddTransaction());
        setBudget.addActionListener(e -> openBudgets());
        report.addActionListener(e -> openPage("AnalyticsFrame"));

        panel.add(quickAdd);
        panel.add(setBudget);
        panel.add(report);

        return panel;
    }

    // OPEN ADD TRANSACTION SCREEN
    private void openAddTransaction() {
        if (token == null || token.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please log in before adding a transaction.",
                    "Login Required",
                    JOptionPane.WARNING_MESSAGE);

            openPage("LoginFrame");
            return;
        }

        new AddTransactionFrame(token).setVisible(true);
    }

    // OPEN BUDGETS SCREEN
    private void openBudgets() {
        if (token == null || token.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please log in before opening Budgets.",
                    "Login Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BudgetsFrame frame = new BudgetsFrame(token);
            frame.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Could not open Budgets page:\n" + ex.getMessage(),
                    "Budgets Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // MONTHLY BUDGET CARD
    private JPanel createBudgetCard() {
        JPanel card = createCard();

        JLabel title = createHeading("Monthly Budget");
        JLabel detail = new JLabel("₹6,350 left of ₹20,000");
        detail.setFont(new Font("Arial", Font.PLAIN, 14));

        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(68);
        progress.setStringPainted(false);
        progress.setForeground(HOVER);
        progress.setBackground(new Color(235, 235, 235));
        progress.setPreferredSize(new Dimension(300, 12));
        progress.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 12));

        JButton manageBudget = createButton("Manage Budgets");
        manageBudget.addActionListener(e -> openBudgets());

        card.add(title);
        card.add(Box.createVerticalStrut(12));
        card.add(detail);
        card.add(Box.createVerticalStrut(12));
        card.add(progress);
        card.add(Box.createVerticalStrut(12));
        card.add(manageBudget);

        return card;
    }

    // RECENT TRANSACTIONS
    private JPanel createTransactionsCard() {
        JPanel card = createCard();

        JLabel title = createHeading("Recent Transactions");

        transactionsPanel = new JPanel();
        transactionsPanel.setLayout(
                new BoxLayout(transactionsPanel, BoxLayout.Y_AXIS));
        transactionsPanel.setBackground(Color.WHITE);

        JLabel loading = new JLabel("Loading transactions...");
        loading.setForeground(Color.GRAY);
        transactionsPanel.add(loading);

        JButton seeAll = createLinkButton(
                "See All Transactions →");
        seeAll.addActionListener(
                e -> openPage("TransactionHistoryFrame"));

        card.add(title);
        card.add(Box.createVerticalStrut(12));
        card.add(transactionsPanel);
        card.add(Box.createVerticalStrut(12));
        card.add(seeAll);

        return card;
    }

    private void addTransaction(
            String title, double amount, boolean isIncome) {

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(new MatteBorder(
                0, 0, 1, 0, new Color(238, 238, 238)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.setPreferredSize(new Dimension(500, 42));

        JLabel name = new JLabel(title);
        name.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel value = new JLabel(
                (isIncome ? "+" : "-") + "₹" + money.format(amount));
        value.setFont(new Font("Arial", Font.BOLD, 14));
        value.setForeground(isIncome ? INCOME_GREEN : RED);

        row.add(name, BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);

        transactionsPanel.add(row);
    }

    // CATEGORY BREAKDOWN
    private JPanel createCategoryCard() {
        JPanel card = createCard();

        JLabel title = createHeading("Category Breakdown");

        JLabel message = new JLabel(
                "<html><div style='width:350px;color:#666666'>"
                + "Category breakdown will be available after "
                + "connecting the analytics page."
                + "</div></html>");

        message.setFont(new Font("Arial", Font.PLAIN, 14));

        JButton insights = createButton("View Insights");
        insights.addActionListener(
                e -> openPage("AnalyticsFrame"));

        card.add(title);
        card.add(Box.createVerticalStrut(12));
        card.add(message);
        card.add(Box.createVerticalStrut(15));
        card.add(insights);

        return card;
    }

    // BOTTOM NAVIGATION
    private JPanel createNavigation() {
        JPanel nav = new JPanel(new GridLayout(1, 5, 4, 0));
        nav.setBackground(Color.WHITE);

        nav.setBorder(new CompoundBorder(
                new MatteBorder(
                        1, 0, 0, 0, new Color(235, 235, 235)),
                new EmptyBorder(8, 4, 8, 4)));

        nav.add(createNavButton("⌂", "Home", null));

        nav.add(createNavButton(
                "▤", "History", "TransactionHistoryFrame"));

        nav.add(createNavButton(
                "+", "Quick Add", "AddTransactionFrame"));

        nav.add(createNavButton(
                "▥", "Insights", "AnalyticsFrame"));

        nav.add(createNavButton(
                "◉", "Budgets", "BudgetsFrame"));

        return nav;
    }

    private JButton createNavButton(
            String symbol, String text, String page) {

        JButton button = new JButton(
                "<html><center><font size='5'>" + symbol
                + "</font><br><font size='2'>" + text
                + "</font></center></html>");

        button.setForeground(GREEN);
        button.setBackground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (page != null) {
            if ("AddTransactionFrame".equals(page)) {
                button.addActionListener(e -> openAddTransaction());
            } else if ("BudgetsFrame".equals(page)) {
                button.addActionListener(e -> openBudgets());
            } else {
                button.addActionListener(e -> openPage(page));
            }
        }

        return button;
    }

    // COMMON CARD
    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);

        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(242, 242, 240), 1, true),
                new EmptyBorder(18, 18, 18, 18)));

        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        return card;
    }

    private JLabel createHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 19));
        label.setForeground(new Color(26, 28, 27));

        return label;
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(GREEN);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(11, 17, 11, 17));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(GREEN);
            }
        });

        return button;
    }

    private JButton createLinkButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(HOVER);
        button.setBackground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        return button;
    }

    // LOAD DASHBOARD DATA
    private void loadDashboard() {
        SwingWorker<String[], Void> worker =
                new SwingWorker<String[], Void>() {

            @Override
            protected String[] doInBackground() throws Exception {
                String summary = apiGet("/summary");
                String transactions = apiGet("/transactions");

                return new String[]{summary, transactions};
            }

            @Override
            protected void done() {
                try {
                    String[] result = get();
                    updateSummary(result[0]);
                    updateTransactions(result[1]);

                } catch (Exception ex) {
                    setValues(
                            "Unable to load",
                            "Unavailable",
                            "Unavailable",
                            "Unavailable");

                    showTransactionMessage(
                            "Unable to load transactions.");

                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private String apiGet(String endpoint) throws Exception {
        URL url = new URL(API_URL + endpoint);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty(
                "Authorization", "Bearer " + token);

        try {
            int status = connection.getResponseCode();

            if (status == 401) {
                throw new IOException(
                        "Unauthorized. Please log in again.");
            }

            if (status < 200 || status >= 300) {
                throw new IOException("Request failed: " + status);
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream(),
                            StandardCharsets.UTF_8))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return response.toString();

        } finally {
            connection.disconnect();
        }
    }

    // SIMPLE JSON VALUE READER
    private String jsonValue(String json, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key)
                + "\"\\s*:\\s*(?:\"((?:\\\\.|[^\"])*)\""
                + "|(-?[0-9]+(?:\\.[0-9]+)?))");

        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            if (matcher.group(1) != null) {
                return matcher.group(1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n");
            }

            return matcher.group(2);
        }

        return "0";
    }

    private double number(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void updateSummary(String json) {
        double income = number(jsonValue(json, "income"));
        double expenses = number(jsonValue(json, "expenses"));
        double balance = income - expenses;

        setValues(
                "₹" + money.format(balance),
                "₹" + money.format(income),
                "₹" + money.format(expenses),
                "₹" + money.format(balance));
    }

    private void setValues(
            String balance,
            String income,
            String expenses,
            String saved) {

        balanceLabel.setText(balance);
        incomeLabel.setText(income);
        expenseLabel.setText(expenses);
        savedLabel.setText(saved);
    }

    // DISPLAY RECENT TRANSACTIONS
    private void updateTransactions(String json) {
        transactionsPanel.removeAll();

        Matcher matcher =
                Pattern.compile("\\{[^{}]*\\}").matcher(json);

        int count = 0;

        while (matcher.find() && count < 3) {
            String object = matcher.group();

            if (!object.contains("\"amount\"")) {
                continue;
            }

            String title = jsonValue(object, "title");
            String type = jsonValue(object, "type");
            double amount = number(jsonValue(object, "amount"));

            boolean isIncome =
                    "income".equalsIgnoreCase(type);

            if ("0".equals(title) || title.isEmpty()) {
                title = "Transaction";
            }

            addTransaction(title, amount, isIncome);
            count++;
        }

        if (count == 0) {
            JLabel empty = new JLabel(
                    "No transactions yet. Add your first transaction!");

            empty.setForeground(Color.GRAY);
            empty.setBorder(new EmptyBorder(12, 0, 12, 0));
            transactionsPanel.add(empty);
        }

        transactionsPanel.revalidate();
        transactionsPanel.repaint();
    }

    private void showTransactionMessage(String text) {
        transactionsPanel.removeAll();

        JLabel message = new JLabel(text);
        message.setForeground(Color.GRAY);

        transactionsPanel.add(message);
        transactionsPanel.revalidate();
        transactionsPanel.repaint();
    }

    // NAVIGATION
    private void openPage(String className) {
        if ("DashboardFrame".equals(className)) {
            return;
        }

        if ("AddTransactionFrame".equals(className)) {
            openAddTransaction();
            return;
        }

        if ("BudgetsFrame".equals(className)) {
            openBudgets();
            return;
        }

        if ("ProfileFrame".equals(className)) {
            openProfile();
            return;
        }

        if ("LoginFrame".equals(className)) {
            dispose();
            new LoginFrame().setVisible(true);
            return;
        }

        try {
            Class<?> clazz =
                    Class.forName("spendly." + className);

            JFrame frame;

            try {
                frame = (JFrame) clazz
                        .getDeclaredConstructor(String.class)
                        .newInstance(token);

            } catch (NoSuchMethodException ex) {
                frame = (JFrame) clazz
                        .getDeclaredConstructor()
                        .newInstance();
            }

            frame.setVisible(true);
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    className + " is not ready yet, or its constructor "
                    + "does not match.\n\nDetails: " + ex.getMessage(),
                    "Spendly",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // LOGOUT
    private void logout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Logout",
                JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Void, Void> worker =
                new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                URL url = new URL(API_URL + "/logout");

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                connection.setRequestProperty(
                        "Authorization", "Bearer " + token);

                try {
                    connection.getResponseCode();
                } finally {
                    connection.disconnect();
                }

                return null;
            }

            @Override
            protected void done() {
                token = null;
                dispose();
                new LoginFrame().setVisible(true);
            }
        };

        worker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new DashboardFrame(null).setVisible(true));
    }
}

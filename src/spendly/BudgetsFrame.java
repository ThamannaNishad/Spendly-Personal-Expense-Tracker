package spendly;

import com.google.gson.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.List;

public class BudgetsFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String API = "http://localhost:8080";

    private static final Color DARK_GREEN = new Color(22, 52, 40);
    private static final Color GREEN = new Color(67, 102, 77);
    private static final Color PALE_GREEN = new Color(228, 240, 231);
    private static final Color BACKGROUND = new Color(249, 249, 247);
    private static final Color RED = new Color(186, 26, 26);
    private static final Color MUTED = new Color(114, 121, 116);

    private final String token;
    private final Gson gson = new Gson();

    private final List<String> categories = Arrays.asList(
        "Food", "Transportation", "Shopping", "Education",
        "Entertainment", "Bills", "Health", "Care", "Subs", "Other"
    );

    private final Map<String, Double> defaultLimits = new LinkedHashMap<>();
    private final Map<String, Double> limits = new LinkedHashMap<>();
    private final Map<String, Double> categorySpent = new LinkedHashMap<>();

    private final List<JsonObject> transactions = new ArrayList<>();
    private final List<JsonObject> goals = new ArrayList<>();

    private double monthlyBudget = 8000;
    private double monthlyIncome;
    private double monthlySpent;

    private final JPanel budgetPanel = new JPanel();
    private final JPanel savingsPanel = new JPanel();

    private final JLabel totalBudgetLabel = new JLabel("₹8,000");
    private final JLabel totalSpentLabel = new JLabel("Spent: ₹0");
    private final JLabel remainingLabel = new JLabel("Remaining: ₹8,000");
    private final JLabel budgetMessageLabel = new JLabel("Loading...");

    private final JLabel balanceIncomeLabel = new JLabel("₹0");
    private final JLabel balanceExpensesLabel = new JLabel("₹0");
    private final JLabel balanceLabel = new JLabel("₹0");

    private final JLabel statusLabel = new JLabel(" ");
    private final JProgressBar overallProgress = new JProgressBar(0, 100);

    public BudgetsFrame(String token) {
        this.token = token;

        if (token == null || token.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "Please log in before opening Budgets.",
                "Login Required",
                JOptionPane.WARNING_MESSAGE
            );
            dispose();
            return;
        }

        defaultLimits.put("Food", 2000.0);
        defaultLimits.put("Transportation", 1000.0);
        defaultLimits.put("Shopping", 1000.0);
        defaultLimits.put("Education", 1500.0);
        defaultLimits.put("Entertainment", 500.0);
        defaultLimits.put("Bills", 500.0);
        defaultLimits.put("Health", 500.0);
        defaultLimits.put("Care", 300.0);
        defaultLimits.put("Subs", 200.0);
        defaultLimits.put("Other", 500.0);

        limits.putAll(defaultLimits);

        for (String category : categories) {
            categorySpent.put(category, 0.0);
        }

        setTitle("Spendly - Budgets & Savings Goals");
        setSize(900, 760);
        setMinimumSize(new Dimension(650, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadAllData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND);
        header.setBorder(new EmptyBorder(16, 20, 10, 20));

        JLabel brand = new JLabel("Spendly");
        brand.setFont(new Font("SansSerif", Font.BOLD, 26));
        brand.setForeground(DARK_GREEN);

        JButton refresh = new JButton("Refresh");
        styleSecondary(refresh);
        refresh.addActionListener(e -> loadAllData());

        header.add(brand, BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();

        budgetPanel.setLayout(
            new BoxLayout(budgetPanel, BoxLayout.Y_AXIS)
        );
        budgetPanel.setBackground(BACKGROUND);
        budgetPanel.setBorder(new EmptyBorder(16, 16, 20, 16));

        savingsPanel.setLayout(
            new BoxLayout(savingsPanel, BoxLayout.Y_AXIS)
        );
        savingsPanel.setBackground(BACKGROUND);
        savingsPanel.setBorder(new EmptyBorder(16, 16, 20, 16));

        JScrollPane budgetScroll = new JScrollPane(budgetPanel);
        budgetScroll.getVerticalScrollBar().setUnitIncrement(16);
        budgetScroll.setBorder(null);

        JScrollPane savingsScroll = new JScrollPane(savingsPanel);
        savingsScroll.getVerticalScrollBar().setUnitIncrement(16);
        savingsScroll.setBorder(null);

        tabs.addTab("Monthly Budgets", budgetScroll);
        tabs.addTab("Savings Goals", savingsScroll);

        statusLabel.setBorder(new EmptyBorder(6, 16, 8, 16));
        statusLabel.setForeground(MUTED);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BACKGROUND);
        footer.add(statusLabel, BorderLayout.NORTH);
        footer.add(createNavigation(), BorderLayout.SOUTH);

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);

        renderBudgets();
        renderGoals();
    }

    private JPanel createNavigation() {
        JPanel nav = new JPanel(new GridLayout(1, 5, 6, 0));
        nav.setBackground(Color.WHITE);
        nav.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton home = createNavButton("Home");
        JButton history = createNavButton("History");
        JButton add = createNavButton("Quick Add");
        JButton insights = createNavButton("Insights");
        JButton budgets = createNavButton("Budgets");

        home.addActionListener(e -> openPage("Home"));
        history.addActionListener(e -> openPage("History"));
        add.addActionListener(e -> openPage("Add"));
        insights.addActionListener(e -> openPage("Insights"));
        budgets.addActionListener(e -> openPage("Budgets"));

        stylePrimary(budgets);

        nav.add(home);
        nav.add(history);
        nav.add(add);
        nav.add(insights);
        nav.add(budgets);

        return nav;
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        styleSecondary(button);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setMargin(new Insets(8, 4, 8, 4));
        return button;
    }

    private void openPage(String page) {
        if ("Budgets".equals(page)) {
            return;
        }

        try {
            JFrame next;

            switch (page) {
                case "Home":
                    next = new DashboardFrame(token);
                    break;

                case "History":
                    next = new TransactionHistoryFrame(token);
                    break;

                case "Add":
                    next = new AddTransactionFrame(token);
                    break;

                case "Insights":
                    next = new AnalyticsFrame(token);
                    break;

                default:
                    showError("Unknown page: " + page);
                    return;
            }

            next.setLocationRelativeTo(null);
            next.setVisible(true);
            dispose();

        } catch (Exception ex) {
            showError(
                "Could not open " + page + ": " + errorMessage(ex)
            );
        }
    }

    private JPanel card() {
        JPanel panel = new JPanel();

        panel.setLayout(
            new BoxLayout(panel, BoxLayout.Y_AXIS)
        );

        panel.setBackground(Color.WHITE);

        panel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(235, 235, 232)
                ),
                new EmptyBorder(16, 18, 16, 18)
            )
        );

        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1000));

        return panel;
    }

    private JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 17));
        label.setForeground(DARK_GREEN);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel largeAmount(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 28));
        label.setForeground(DARK_GREEN);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(MUTED);
        return label;
    }

    private JPanel row(Component left, Component right) {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    private void stylePrimary(JButton button) {
        button.setBackground(DARK_GREEN);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 14, 9, 14));
        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }

    private void styleSecondary(JButton button) {
        button.setBackground(PALE_GREEN);
        button.setForeground(DARK_GREEN);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 13, 8, 13));
        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }
    private void renderBudgets() {
        budgetPanel.removeAll();

        JPanel incomeCard = card();
        incomeCard.add(title("MONTHLY INCOME"));
        incomeCard.add(Box.createVerticalStrut(10));
        incomeCard.add(largeAmount(money(monthlyIncome)));
        incomeCard.add(muted("Income recorded this month"));
        budgetPanel.add(incomeCard);
        budgetPanel.add(Box.createVerticalStrut(12));

        double remaining = monthlyBudget - monthlySpent;
        int percent = percentage(monthlySpent, monthlyBudget);

        totalBudgetLabel.setText(money(monthlyBudget));
        totalSpentLabel.setText("Spent: " + money(monthlySpent));
        remainingLabel.setText("Remaining: " + money(remaining));

        JPanel totalCard = card();
        totalCard.add(title("TOTAL MONTHLY BUDGET"));
        totalCard.add(Box.createVerticalStrut(10));
        totalCard.add(largeAmount(totalBudgetLabel.getText()));
        totalCard.add(Box.createVerticalStrut(12));
        totalCard.add(row(totalSpentLabel, remainingLabel));

        overallProgress.setValue(percent);
        overallProgress.setString(percent + "%");
        overallProgress.setStringPainted(true);
        overallProgress.setForeground(
            monthlySpent > monthlyBudget ? RED : GREEN
        );
        overallProgress.setBackground(new Color(232, 232, 230));
        overallProgress.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 18)
        );

        totalCard.add(Box.createVerticalStrut(10));
        totalCard.add(overallProgress);
        totalCard.add(Box.createVerticalStrut(10));

        budgetMessageLabel.setText(budgetMessage());
        budgetMessageLabel.setForeground(
            monthlySpent > monthlyBudget ? RED : GREEN
        );
        totalCard.add(budgetMessageLabel);
        totalCard.add(Box.createVerticalStrut(10));

        JButton setBudget = new JButton("Set Monthly Budget");
        styleSecondary(setBudget);
        setBudget.setAlignmentX(Component.LEFT_ALIGNMENT);
        setBudget.addActionListener(e -> editMonthlyBudget());
        totalCard.add(setBudget);

        budgetPanel.add(totalCard);
        budgetPanel.add(Box.createVerticalStrut(12));

        double actualBalance = monthlyIncome - monthlySpent;
        balanceIncomeLabel.setText(money(monthlyIncome));
        balanceExpensesLabel.setText(money(monthlySpent));
        balanceLabel.setText(money(actualBalance));
        balanceLabel.setForeground(actualBalance < 0 ? RED : GREEN);

        JPanel balanceCard = card();
        balanceCard.add(title("Monthly Balance"));
        balanceCard.add(Box.createVerticalStrut(12));
        balanceCard.add(row(muted("Income"), balanceIncomeLabel));
        balanceCard.add(Box.createVerticalStrut(8));
        balanceCard.add(row(muted("Expenses"), balanceExpensesLabel));
        balanceCard.add(Box.createVerticalStrut(8));
        balanceCard.add(row(muted("Balance after expenses"), balanceLabel));

        budgetPanel.add(balanceCard);
        budgetPanel.add(Box.createVerticalStrut(18));

        JPanel categoryHeader = new JPanel(new BorderLayout());
        categoryHeader.setOpaque(false);
        categoryHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryHeader.add(title("Category Limits"), BorderLayout.WEST);

        JButton addLimit = new JButton("+ Set Limit");
        stylePrimary(addLimit);
        addLimit.addActionListener(e -> editCategoryBudget());
        categoryHeader.add(addLimit, BorderLayout.EAST);

        budgetPanel.add(categoryHeader);
        budgetPanel.add(Box.createVerticalStrut(6));
        budgetPanel.add(
            muted("Spending is calculated from recorded transactions.")
        );
        budgetPanel.add(Box.createVerticalStrut(12));

        for (String category : categories) {
            budgetPanel.add(categoryCard(category));
            budgetPanel.add(Box.createVerticalStrut(10));
        }

        budgetPanel.revalidate();
        budgetPanel.repaint();
    }

    private JPanel categoryCard(String category) {
        JPanel panel = card();

        double spent = categorySpent.getOrDefault(category, 0.0);
        double limit = limits.getOrDefault(category, 0.0);
        double remaining = limit - spent;
        int percent = percentage(spent, limit);

        JLabel status = new JLabel();

        if (limit <= 0) {
            status.setText("No limit");
            status.setForeground(MUTED);
        } else if (spent > limit) {
            status.setText("Over Budget");
            status.setForeground(RED);
        } else if (percent >= 80) {
            status.setText(percent + "% Spent");
            status.setForeground(new Color(180, 95, 0));
        } else {
            status.setText("On Track");
            status.setForeground(GREEN);
        }

        panel.add(row(title(category), status));
        panel.add(Box.createVerticalStrut(8));
        panel.add(muted("Budget: " + money(limit)));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(percent);
        bar.setString(percent + "%");
        bar.setStringPainted(true);
        bar.setForeground(
            spent > limit ? RED
                : (percent >= 80 ? new Color(217, 119, 6) : GREEN)
        );
        bar.setBackground(new Color(232, 232, 230));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 17));

        panel.add(Box.createVerticalStrut(8));
        panel.add(bar);
        panel.add(Box.createVerticalStrut(8));

        JLabel left = new JLabel(
            remaining < 0
                ? money(Math.abs(remaining)) + " over"
                : money(remaining) + " left"
        );
        left.setForeground(remaining < 0 ? RED : GREEN);

        panel.add(row(muted("Spent: " + money(spent)), left));
        panel.add(Box.createVerticalStrut(10));

        JButton edit = new JButton("Edit Limit");
        styleSecondary(edit);
        edit.setAlignmentX(Component.LEFT_ALIGNMENT);
        edit.addActionListener(e -> editCategoryBudget(category));
        panel.add(edit);

        return panel;
    }

    private String budgetMessage() {
        if (monthlyBudget <= 0) {
            return "Set a monthly budget to track spending.";
        }

        if (monthlySpent > monthlyBudget) {
            return "You are over budget by "
                + money(monthlySpent - monthlyBudget) + ".";
        }

        return "You have "
            + money(monthlyBudget - monthlySpent)
            + " of your budget remaining.";
    }

    private void editMonthlyBudget() {
        Double value = askAmount(
            "Enter your monthly spending budget:",
            monthlyBudget,
            true
        );

        if (value == null) {
            return;
        }

        double old = monthlyBudget;
        monthlyBudget = value;

        if (saveBudgets()) {
            renderBudgets();
        } else {
            monthlyBudget = old;
        }
    }

    private void editCategoryBudget(String category) {
        if (category == null || !limits.containsKey(category)) {
            return;
        }

        double old = limits.getOrDefault(category, 0.0);

        Double value = askAmount(
            "Enter the budget limit for " + category + ":",
            old,
            true
        );

        if (value == null) {
            return;
        }

        limits.put(category, value);

        if (saveBudgets()) {
            renderBudgets();
        } else {
            limits.put(category, old);
        }
    }

    private void editCategoryBudget() {
        JComboBox<String> combo = new JComboBox<>(
            categories.toArray(new String[0])
        );

        int result = JOptionPane.showConfirmDialog(
            this,
            combo,
            "Select Category",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            editCategoryBudget((String) combo.getSelectedItem());
        }
    }

    private Double askAmount(
        String message,
        double initialValue,
        boolean allowZero
    ) {
        String input = JOptionPane.showInputDialog(
            this,
            message,
            String.valueOf(initialValue)
        );

        if (input == null) {
            return null;
        }

        try {
            double value = Double.parseDouble(input.trim());

            if (!Double.isFinite(value)
                    || value < 0
                    || (!allowZero && value == 0)) {
                throw new NumberFormatException();
            }

            return value;

        } catch (NumberFormatException ex) {
            showError("Please enter a valid amount.");
            return null;
        }
    }

    private void renderGoals() {
        savingsPanel.removeAll();

        savingsPanel.add(title("Savings Goals"));
        savingsPanel.add(Box.createVerticalStrut(8));
        savingsPanel.add(
            muted("Create a goal and track how much you have saved.")
        );
        savingsPanel.add(Box.createVerticalStrut(14));

        if (goals.isEmpty()) {
            savingsPanel.add(
                muted("No savings goals yet. Create your first goal!")
            );
            savingsPanel.add(Box.createVerticalStrut(15));
        }

        for (JsonObject goal : new ArrayList<>(goals)) {
            savingsPanel.add(goalCard(goal));
            savingsPanel.add(Box.createVerticalStrut(12));
        }

        JButton create = new JButton("+ Create New Goal");
        stylePrimary(create);
        create.setAlignmentX(Component.LEFT_ALIGNMENT);
        create.addActionListener(e -> createGoal());

        savingsPanel.add(create);
        savingsPanel.add(Box.createVerticalStrut(12));
        savingsPanel.add(
            muted("Savings goals are saved in your account database.")
        );

        savingsPanel.revalidate();
        savingsPanel.repaint();
    }

    private JPanel goalCard(JsonObject goal) {
        JPanel panel = card();

        int id = getInt(goal, "id", -1);
        String name = getString(goal, "name", "Savings Goal");
        double target = getDouble(goal, "target", 0);
        double saved = getDouble(goal, "saved", 0);
        double remaining = Math.max(0, target - saved);
        int percent = percentage(saved, target);

        panel.add(title(name));
        panel.add(Box.createVerticalStrut(7));
        panel.add(muted("Target: " + money(target)));
        panel.add(Box.createVerticalStrut(10));
        panel.add(row(
            new JLabel(money(saved) + " saved"),
            new JLabel(percent + "%")
        ));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(percent);
        bar.setString(percent + "%");
        bar.setStringPainted(true);
        bar.setForeground(GREEN);
        bar.setBackground(new Color(232, 232, 230));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 17));

        panel.add(Box.createVerticalStrut(8));
        panel.add(bar);
        panel.add(Box.createVerticalStrut(8));
        panel.add(
            muted(remaining > 0
                ? money(remaining) + " remaining"
                : "Goal completed!")
        );
        panel.add(Box.createVerticalStrut(10));

        JPanel actions = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 6, 0)
        );
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addMoney = new JButton("+ Add Money");
        styleSecondary(addMoney);
        addMoney.addActionListener(e -> addGoalMoney(id, target, saved));

        JButton delete = new JButton("Delete");
        styleSecondary(delete);
        delete.addActionListener(e -> deleteGoal(id, name));

        actions.add(addMoney);
        actions.add(delete);
        panel.add(actions);

        return panel;
    }
    private void createGoal() {
        JTextField nameField = new JTextField(20);
        JTextField targetField = new JTextField("10000", 12);

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.add(new JLabel("Goal name:"));
        form.add(nameField);
        form.add(new JLabel("Target amount (₹):"));
        form.add(targetField);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "Create Savings Goal",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            showError("Please enter a goal name.");
            return;
        }

        double target;

        try {
            target = Double.parseDouble(targetField.getText().trim());

            if (!Double.isFinite(target) || target <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException ex) {
            showError("Enter a valid target amount greater than zero.");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("target", target);
        body.addProperty("saved", 0);

        try {
            JsonObject response = requestObject("POST", "/goals", body);

            if (!response.has("id") || response.get("id").isJsonNull()) {
                throw new IOException("Server response is missing the goal ID.");
            }

            goals.add(response);
            clearStatus();
            renderGoals();

        } catch (Exception ex) {
            showError("Could not create goal: " + errorMessage(ex));
        }
    }

    private void addGoalMoney(int id, double target, double saved) {
        double remaining = target - saved;

        if (remaining <= 0) {
            showInfo("Congratulations! Goal completed.");
            return;
        }

        Double amount = askAmount(
            "Amount to add. Remaining: " + money(remaining),
            Math.min(500, remaining),
            false
        );

        if (amount == null) {
            return;
        }

        if (amount > remaining) {
            showError("Amount cannot exceed the remaining goal amount.");
            return;
        }

        if (id < 0) {
            showError("The server did not return a valid goal ID.");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("saved", saved + amount);

        try {
            JsonObject updated = requestObject(
                "PUT",
                "/goals/" + id,
                body
            );

            replaceGoal(updated);
            clearStatus();
            renderGoals();

        } catch (Exception ex) {
            showError("Could not update goal: " + errorMessage(ex));
        }
    }

    private void deleteGoal(int id, String name) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete savings goal \"" + name + "\"?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if (id < 0) {
            showError("The server did not return a valid goal ID.");
            return;
        }

        try {
            request("DELETE", "/goals/" + id, null);

            goals.removeIf(
                goal -> getInt(goal, "id", -1) == id
            );

            clearStatus();
            renderGoals();

        } catch (Exception ex) {
            showError("Could not delete goal: " + errorMessage(ex));
        }
    }


    private void replaceGoal(JsonObject updated) {
        int id = getInt(updated, "id", -1);

        for (int i = 0; i < goals.size(); i++) {
            if (getInt(goals.get(i), "id", -2) == id) {
                goals.set(i, updated);
                return;
            }
        }

        goals.add(updated);
    }

    private void loadAllData() {
        statusLabel.setForeground(MUTED);
        statusLabel.setText("Loading data...");

        SwingWorker<Void, Void> worker =
            new SwingWorker<Void, Void>() {

            private String error;

            @Override
            protected Void doInBackground() {
                try {
                    loadTransactions();
                    loadBudgets();
                    loadGoals();
                    calculateSpending();

                } catch (Exception ex) {
                    error = errorMessage(ex);
                }

                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    showError("Could not load data: " + error);
                } else {
                    clearStatus();
                }

                renderBudgets();
                renderGoals();
            }
        };

        worker.execute();
    }


private void loadTransactions() throws Exception {
    JsonElement response = request("GET", "/transactions", null);

    System.out.println("\n========== TRANSACTIONS API ==========");
    System.out.println("API response: " + response);

    transactions.clear();

    JsonArray array = null;

    if (response != null && response.isJsonArray()) {
        array = response.getAsJsonArray();

    } else if (response != null && response.isJsonObject()) {
        JsonObject object = response.getAsJsonObject();

        if (object.has("transactions")
                && object.get("transactions").isJsonArray()) {
            array = object.getAsJsonArray("transactions");
        }
    }

    if (array != null) {
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                transactions.add(element.getAsJsonObject());
            }
        }
    }

    System.out.println("Transactions loaded: " + transactions.size());
    System.out.println("======================================\n");
}

      

    private void loadBudgets() throws Exception {
        JsonElement response = request("GET", "/budgets", null);

        if (response == null || !response.isJsonObject()) {
            return;
        }

        JsonObject object = response.getAsJsonObject();

        monthlyBudget = getDouble(object, "monthlyBudget", 8000);

        if (object.has("limits")
                && object.get("limits").isJsonObject()) {

            JsonObject savedLimits = object.getAsJsonObject("limits");

            for (String category : categories) {
                limits.put(
                    category,
                    getDouble(
                        savedLimits,
                        category,
                        defaultLimits.get(category)
                    )
                );
            }
        }
    }

    private void loadGoals() throws Exception {
        JsonElement response = request("GET", "/goals", null);

        goals.clear();

        JsonArray array = null;

        if (response != null && response.isJsonArray()) {
            array = response.getAsJsonArray();

        } else if (response != null && response.isJsonObject()) {
            JsonObject object = response.getAsJsonObject();

            if (object.has("goals")
                    && object.get("goals").isJsonArray()) {
                array = object.getAsJsonArray("goals");
            }
        }

        if (array != null) {
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    goals.add(element.getAsJsonObject());
                }
            }
        }
    }



private void calculateSpending() {
    monthlyIncome = 0;
    monthlySpent = 0;

    // Reset category spending
    for (String category : categories) {
        categorySpent.put(category, 0.0);
    }

    YearMonth currentMonth = YearMonth.now();

    System.out.println("\n========== BUDGET DEBUG ==========");
    System.out.println("Current month: " + currentMonth);
    System.out.println("Transactions loaded: " + transactions.size());

    for (JsonObject transaction : transactions) {

        String type = getString(
            transaction,
            "type",
            ""
        ).trim().toLowerCase(Locale.ROOT);

        double amount = getDouble(
            transaction,
            "amount",
            0
        );

        String category = normalizeCategory(
            getString(transaction, "category", "Other")
        );

        LocalDate date = transactionDate(transaction);

        System.out.println(
            "Transaction: type=" + type
            + ", amount=" + amount
            + ", category=" + category
            + ", date=" + date
        );

        // Count only transactions from the current month.
        if (date != null
                && !YearMonth.from(date).equals(currentMonth)) {
            System.out.println(
                "Skipped: transaction belongs to another month."
            );
            continue;
        }

        // Ignore zero or negative amounts.
        if (amount <= 0) {
            System.out.println(
                "Skipped: amount is zero or negative."
            );
            continue;
        }

        if ("income".equals(type)) {

            monthlyIncome += amount;

        } else if ("expense".equals(type)) {

            monthlySpent += amount;

            categorySpent.put(
                category,
                categorySpent.getOrDefault(category, 0.0) + amount
            );

        } else {

            System.out.println(
                "Unknown transaction type: " + type
            );
        }
    }

    // Print calculated totals for debugging.
    System.out.println("Monthly income: " + monthlyIncome);
    System.out.println("Monthly spent: " + monthlySpent);
    System.out.println("Category spending: " + categorySpent);
    System.out.println("==================================\n");
}

    private LocalDate transactionDate(JsonObject transaction) {
        String[] fields = {
            "date",
            "transaction_date",
            "transactionDate",
            "created_at",
            "createdAt"
        };

        for (String field : fields) {
            if (!transaction.has(field)
                    || transaction.get(field).isJsonNull()) {
                continue;
            }

            String value = transaction.get(field).getAsString().trim();

            if (value.length() >= 10) {
                try {
                    return LocalDate.parse(value.substring(0, 10));
                } catch (Exception ignored) {
                    // Try the next date field.
                }
            }
        }

        return null;
    }

    private String normalizeCategory(String value) {
        String name = value == null
            ? "Other"
            : value.trim().toLowerCase(Locale.ROOT);

        if (name.equals("food & drinks")
                || name.equals("food and drinks")) {
            return "Food";
        }

        if (name.equals("transport")) {
            return "Transportation";
        }

        if (name.equals("education & supplies")) {
            return "Education";
        }

        if (name.equals("fun")) {
            return "Entertainment";
        }

        if (name.equals("subscription")
                || name.equals("subscriptions")) {
            return "Subs";
        }

        for (String category : categories) {
            if (category.equalsIgnoreCase(name)) {
                return category;
            }
        }

        return "Other";
    }

    private boolean saveBudgets() {
        JsonObject body = new JsonObject();
        body.addProperty("monthlyBudget", monthlyBudget);

        JsonObject limitObject = new JsonObject();

        for (String category : categories) {
            limitObject.addProperty(
                category,
                limits.getOrDefault(category, 0.0)
            );
        }

        body.add("limits", limitObject);

        try {
            request("PUT", "/budgets", body);
            clearStatus();
            return true;

        } catch (Exception ex) {
            showError("Could not save budgets: " + errorMessage(ex));
            return false;
        }
    }

    private JsonElement request(
        String method,
        String path,
        JsonObject body
    ) throws Exception {

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                new URL(API + path).openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty(
                "Authorization",
                "Bearer " + token
            );

            connection.setRequestProperty(
                "Accept",
                "application/json"
            );

            if (body != null) {
                connection.setDoOutput(true);

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                );

                byte[] data = gson.toJson(body)
                    .getBytes(StandardCharsets.UTF_8);

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(data);
                }
            }
            System.out.println("Calling API: " + method + " " + API + path);

            int status = connection.getResponseCode();
            System.out.println("API HTTP status: " + status);

            InputStream stream =
                status >= 200 && status < 400
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String responseBody = "";

            if (stream != null) {
                try (
                    InputStream input = stream;
                    ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
                ) {
                    byte[] buffer = new byte[4096];
                    int count;

                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                    }

                    responseBody = new String(
                        output.toByteArray(),
                        StandardCharsets.UTF_8
                    );
                }
            }

            if (status == 401 || status == 403) {
                throw new IOException(
                    "Authentication failed (HTTP " + status
                    + "). Please log in again."
                );
            }

            if (status < 200 || status >= 300) {
                throw new IOException(
                    "HTTP " + status
                    + (responseBody.isEmpty() ? "" : ": " + responseBody)
                );
            }

            if (responseBody.trim().isEmpty()) {
                return null;
            }

            return JsonParser.parseString(responseBody);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JsonObject requestObject(
        String method,
        String path,
        JsonObject body
    ) throws Exception {

        JsonElement response = request(method, path, body);

        if (response != null && response.isJsonObject()) {
            return response.getAsJsonObject();
        }

        throw new IOException("Expected a JSON object from " + path);
    }

    private int percentage(double amount, double limit) {
        if (limit <= 0) {
            return amount > 0 ? 100 : 0;
        }

        return (int) Math.min(
            100,
            Math.round(amount / limit * 100)
        );
    }

    private String money(double amount) {
        NumberFormat format = NumberFormat.getNumberInstance(
            new Locale("en", "IN")
        );

        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);

        return "₹" + format.format(amount);
    }

    private String getString(
        JsonObject object,
        String key,
        String fallback
    ) {
        if (object == null
                || !object.has(key)
                || object.get(key).isJsonNull()) {
            return fallback;
        }

        try {
            return object.get(key).getAsString();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double getDouble(
        JsonObject object,
        String key,
        double fallback
    ) {
        if (object == null
                || !object.has(key)
                || object.get(key).isJsonNull()) {
            return fallback;
        }

        try {
            return object.get(key).getAsDouble();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int getInt(
        JsonObject object,
        String key,
        int fallback
    ) {
        if (object == null
                || !object.has(key)
                || object.get(key).isJsonNull()) {
            return fallback;
        }

        try {
            return object.get(key).getAsInt();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String errorMessage(Exception ex) {
        return ex.getMessage() == null
            ? ex.toString()
            : ex.getMessage();
    }

    private void showError(String message) {
        statusLabel.setForeground(RED);
        statusLabel.setText(
            message == null ? "An error occurred." : message
        );
    }

    private void clearStatus() {
        statusLabel.setForeground(GREEN);
        statusLabel.setText(" ");
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Spendly",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
}

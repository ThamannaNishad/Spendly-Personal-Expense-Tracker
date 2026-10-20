
package spendly;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AddTransactionFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String API_URL = "http://localhost:8080";

    private final String token;

    private String transactionType = "Expense";
    private String selectedCategory = "Food & Drinks";

    private final JButton expenseButton = new JButton("↓ Expense");
    private final JButton incomeButton = new JButton("↑ Income");

    private final JTextField amountField = new JTextField();
    private final JTextField dateField = new JTextField();
    private final JTextField noteField = new JTextField();

    private final JComboBox<String> paymentBox =
            new JComboBox<>(new String[]{
                    "UPI", "Cash", "Debit Card",
                    "Credit Card", "Net Banking", "Other"
            });

    private final JButton saveButton =
            new JButton("✓ Save Transaction");

    private final JLabel statusLabel = new JLabel(" ");

    private final String[] categories = {
            "Food & Drinks",
            "Transportation",
            "Shopping",
            "Education",
            "Entertainment",
            "Bills & Utilities",
            "Health",
            "Personal Care",
            "Subscriptions",
            "Other"
    };

    private final String[] categoryLabels = {
            "Food", "Transit", "Shopping", "Education", "Fun",
            "Bills", "Health", "Care", "Subs", "Other"
    };

    private final JButton[] categoryButtons =
            new JButton[categories.length];

    private final Color primary = new Color(22, 52, 40);
    private final Color secondary = new Color(67, 102, 77);
    private final Color background = new Color(249, 249, 247);
    private final Color inputBackground = new Color(244, 244, 242);

    public AddTransactionFrame(String authToken) {
        this.token = authToken;

        if (authToken == null || authToken.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Please log in before adding a transaction."
            );

            SwingUtilities.invokeLater(() ->
                    new LoginFrame().setVisible(true));

            dispose();
            return;
        }

        setTitle("Spendly - Add Transaction");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 760);
        setMinimumSize(new Dimension(430, 650));
        setLocationRelativeTo(null);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(background);
        root.setBorder(new EmptyBorder(14, 16, 18, 16));
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JButton backButton = new JButton("← Back");
        backButton.setForeground(secondary);
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setFocusPainted(false);

        backButton.addActionListener(e -> {
            dispose();
            new DashboardFrame(token).setVisible(true);
        });

        JLabel brand = new JLabel("Spendly", SwingConstants.CENTER);
        brand.setFont(new Font("SansSerif", Font.BOLD, 20));
        brand.setForeground(primary);

        JLabel quickAdd = new JLabel("Quick Add", SwingConstants.RIGHT);

        header.add(backButton, BorderLayout.WEST);
        header.add(brand, BorderLayout.CENTER);
        header.add(quickAdd, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // Main card
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("New Transaction");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(18));

        // Expense / Income buttons
        JPanel typePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        typePanel.setBackground(inputBackground);
        typePanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        styleTypeButton(expenseButton);
        styleTypeButton(incomeButton);

        expenseButton.setBackground(primary);
        expenseButton.setForeground(Color.WHITE);

        incomeButton.setBackground(inputBackground);
        incomeButton.setForeground(Color.DARK_GRAY);

        expenseButton.addActionListener(e -> switchType("Expense"));
        incomeButton.addActionListener(e -> switchType("Income"));

        typePanel.add(expenseButton);
        typePanel.add(incomeButton);
        typePanel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 52));

        card.add(typePanel);
        card.add(Box.createVerticalStrut(18));

        // Amount section
        JPanel amountPanel = new JPanel();
        amountPanel.setBackground(inputBackground);
        amountPanel.setLayout(new BoxLayout(amountPanel, BoxLayout.Y_AXIS));
        amountPanel.setBorder(new EmptyBorder(18, 12, 18, 12));

        JLabel amountLabel = new JLabel("Enter Amount");
        amountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        amountPanel.add(amountLabel);
        amountPanel.add(Box.createVerticalStrut(8));

        JPanel amountRow = new JPanel(new FlowLayout());
        amountRow.setOpaque(false);

        JLabel rupee = new JLabel("₹");
        rupee.setFont(new Font("SansSerif", Font.BOLD, 30));

        amountField.setFont(new Font("SansSerif", Font.BOLD, 32));
        amountField.setHorizontalAlignment(JTextField.CENTER);
        amountField.setBackground(inputBackground);
        amountField.setForeground(primary);
        amountField.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        amountField.setPreferredSize(new Dimension(240, 48));

        amountRow.add(rupee);
        amountRow.add(amountField);
        amountPanel.add(amountRow);

        // Quick amount buttons
        JPanel quickAmounts = new JPanel(new FlowLayout());
        quickAmounts.setOpaque(false);

        int[] amounts = {50, 100, 500, 1000};

        for (int value : amounts) {
            JButton button = new JButton(
                    "+₹" + String.format("%,d", value));

            button.setFocusPainted(false);
            button.setBackground(Color.WHITE);
            button.setForeground(secondary);

            button.addActionListener(e -> addAmount(value));
            quickAmounts.add(button);
        }

        amountPanel.add(quickAmounts);
        amountPanel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 180));

        card.add(amountPanel);
        card.add(Box.createVerticalStrut(16));

        // Category section
        JLabel categoryTitle = new JLabel("Choose Category");
        categoryTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        categoryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(categoryTitle);
        card.add(Box.createVerticalStrut(10));

        JPanel categoryGrid = new JPanel(new GridLayout(2, 5, 7, 7));
        categoryGrid.setOpaque(false);

        for (int i = 0; i < categories.length; i++) {
            final int index = i;

            JButton button = new JButton(categoryLabels[i]);
            categoryButtons[i] = button;

            button.setFont(new Font("SansSerif", Font.PLAIN, 11));
            button.setMargin(new Insets(12, 2, 12, 2));
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBorderPainted(false);

            button.setBackground(i == 0 ? primary : inputBackground);
            button.setForeground(i == 0 ? Color.WHITE : Color.DARK_GRAY);

            button.addActionListener(
                    e -> selectCategory(index));

            categoryGrid.add(button);
        }

        categoryGrid.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 125));

        card.add(categoryGrid);

        // Date field
        addFieldLabel(card, "Date");

        dateField.setText(
                new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        styleInput(dateField);
        card.add(dateField);

        // Payment method
        addFieldLabel(card, "Payment Method");

        paymentBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        paymentBox.setBackground(inputBackground);
        paymentBox.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 44));
        paymentBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(paymentBox);

        // Description
        addFieldLabel(card, "Note / Description");

        noteField.setToolTipText("e.g. Lunch at canteen");
        styleInput(noteField);
        card.add(noteField);

        // Save button
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        saveButton.setBackground(new Color(20, 83, 45));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setMargin(new Insets(14, 10, 14, 10));
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 52));

        saveButton.addActionListener(e -> saveTransaction());

        card.add(Box.createVerticalStrut(18));
        card.add(saveButton);

        // Status message
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(Box.createVerticalStrut(10));
        card.add(statusLabel);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(background);
        content.add(card, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(background);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        root.add(scrollPane, BorderLayout.CENTER);
    }

    private void addFieldLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(16, 0, 7, 0));
        panel.add(label);
    }

    private void styleInput(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBackground(inputBackground);

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(230, 230, 226)),
                new EmptyBorder(10, 10, 10, 10)));

        field.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void styleTypeButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(10, 5, 10, 5));
    }

    private void switchType(String type) {
        transactionType = type;

        boolean expense = "Expense".equals(type);

        expenseButton.setBackground(
                expense ? primary : inputBackground);
        expenseButton.setForeground(
                expense ? Color.WHITE : Color.DARK_GRAY);

        incomeButton.setBackground(
                expense ? inputBackground : primary);
        incomeButton.setForeground(
                expense ? Color.DARK_GRAY : Color.WHITE);
    }

    private void addAmount(int value) {
        try {
            String text = amountField.getText().trim();

            double current = text.isEmpty()
                    ? 0.0 : Double.parseDouble(text);

            amountField.setText(String.format(
                    java.util.Locale.US, "%.2f", current + value));

        } catch (NumberFormatException ex) {
            amountField.setText(String.format(
                    java.util.Locale.US, "%.2f", value));
        }
    }

    private void selectCategory(int index) {
        selectedCategory = categories[index];

        for (int i = 0; i < categoryButtons.length; i++) {
            boolean selected = i == index;

            categoryButtons[i].setBackground(
                    selected ? primary : inputBackground);

            categoryButtons[i].setForeground(
                    selected ? Color.WHITE : Color.DARK_GRAY);
        }
    }

    private void showStatus(String message, boolean success) {
        statusLabel.setText("<html><div style='width:380px;'>"
                + escapeHtml(message) + "</div></html>");

        statusLabel.setForeground(
                success
                        ? new Color(20, 83, 45)
                        : new Color(153, 27, 27));
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String encode(String value)
            throws java.io.UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }

    private String readAll(InputStream input)
            throws java.io.IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;

        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }

        return new String(
                output.toByteArray(), StandardCharsets.UTF_8);
    }

    private void saveTransaction() {
        String amountText = amountField.getText().trim();
        String note = noteField.getText().trim();
        String date = dateField.getText().trim();

        final double amount;

        if (token == null || token.trim().isEmpty()) {
            showStatus("Please log in again.", false);
            return;
        }

        try {
            if (amountText.isEmpty()) {
                throw new NumberFormatException();
            }

            amount = Double.parseDouble(amountText);

            if (Double.isNaN(amount)
                    || Double.isInfinite(amount)
                    || amount <= 0) {
                showStatus(
                        "Enter an amount greater than zero.", false);
                return;
            }

        } catch (NumberFormatException ex) {
            showStatus("Please enter a valid amount.", false);
            return;
        }

        if (note.isEmpty()) {
            showStatus(
                    "Please enter a transaction description.", false);
            return;
        }

        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            showStatus("Enter date in YYYY-MM-DD format.", false);
            return;
        }

        final String body;

        try {
            body = "title=" + encode(note)
                    + "&amount=" + encode(String.format(
                            java.util.Locale.US, "%.2f", amount))
                    + "&category=" + encode(selectedCategory)
                    + "&type=" + encode(transactionType);

        } catch (java.io.UnsupportedEncodingException ex) {
            showStatus("Could not prepare transaction data.", false);
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        showStatus("Saving transaction...", true);

        SwingWorker<String, Void> worker =
                new SwingWorker<String, Void>() {

            private int statusCode;

            @Override
            protected String doInBackground() throws Exception {
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(API_URL + "/save");
                    connection = (HttpURLConnection) url.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(10000);
                    connection.setDoOutput(true);

                    connection.setRequestProperty(
                            "Content-Type",
                            "application/x-www-form-urlencoded; charset=UTF-8");

                    connection.setRequestProperty(
                            "Authorization", "Bearer " + token);

                    OutputStream output = connection.getOutputStream();
                    output.write(body.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    output.close();

                    statusCode = connection.getResponseCode();

                    InputStream responseStream =
                            statusCode >= 400
                                    ? connection.getErrorStream()
                                    : connection.getInputStream();

                    String responseText = responseStream == null
                            ? "" : readAll(responseStream);

                    if (responseStream != null) {
                        responseStream.close();
                    }

                    return responseText;

                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    String response = get();

                    if (statusCode == 401) {
                        showStatus(
                                "Session expired. Please log in again.",
                                false);

                        dispose();
                        new LoginFrame().setVisible(true);
                        return;
                    }

                    if (statusCode < 200 || statusCode >= 300) {
                        showStatus(
                                "Could not save transaction. HTTP "
                                + statusCode + ". " + response,
                                false);
                        return;
                    }

                    showStatus(
                            "Transaction saved successfully!", true);

                    Timer timer = new Timer(1000, e -> {
                        dispose();
                        new DashboardFrame(token).setVisible(true);
                    });

                    timer.setRepeats(false);
                    timer.start();

                } catch (Exception ex) {
                    showStatus(
                            "Could not connect to the Java server. "
                            + "Check that ExpenseServer is running "
                            + "on port 8080.",
                            false);

                } finally {
                    saveButton.setEnabled(true);
                    saveButton.setText("✓ Save Transaction");
                }
            }
        };

        worker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String token = JOptionPane.showInputDialog(
                    null,
                    "Enter login token for testing:");

            if (token != null && !token.trim().isEmpty()) {
                new AddTransactionFrame(token).setVisible(true);
            }
        });
    }
}

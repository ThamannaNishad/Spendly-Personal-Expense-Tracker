
package spendly;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ProfileFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final String token;

    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JLabel displayName;
    private JLabel avatar;
    private JLabel message;

    private JButton editButton;
    private JButton saveButton;

    private boolean editing = false;

    private final Color darkGreen = new Color(22, 52, 40);
    private final Color green = new Color(36, 91, 70);
    private final Color background = new Color(249, 249, 247);
    private final Color paleGreen = new Color(232, 238, 233);

    public ProfileFrame(String token) {
        this.token = token;

        setTitle("Spendly - Profile");
        setSize(500, 720);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        setFieldsEnabled(false);
        loadProfile();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(background);
        root.setBorder(new EmptyBorder(18, 20, 10, 20));
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Spendly");
        title.setFont(new Font("Arial", Font.BOLD, 27));
        title.setForeground(darkGreen);

        JButton dashboardButton = new JButton("← Dashboard");
        styleSecondaryButton(dashboardButton);
        dashboardButton.addActionListener(e -> goDashboard());

        header.add(title, BorderLayout.WEST);
        header.add(dashboardButton, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Profile card
        JPanel profileCard = new JPanel();
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.setBackground(Color.WHITE);
        profileCard.setBorder(new EmptyBorder(20, 15, 20, 15));
        profileCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        avatar = new JLabel("U", SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(darkGreen);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Arial", Font.BOLD, 34));
        avatar.setPreferredSize(new Dimension(90, 90));
        avatar.setMinimumSize(new Dimension(90, 90));
        avatar.setMaximumSize(new Dimension(90, 90));
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatar.setBorder(BorderFactory.createLineBorder(darkGreen, 1));

        displayName = new JLabel("Your Name", SwingConstants.CENTER);
        displayName.setFont(new Font("Arial", Font.BOLD, 22));
        displayName.setForeground(darkGreen);
        displayName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel memberLabel = new JLabel("Spendly Member");
        memberLabel.setForeground(Color.GRAY);
        memberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        profileCard.add(avatar);
        profileCard.add(Box.createVerticalStrut(12));
        profileCard.add(displayName);
        profileCard.add(Box.createVerticalStrut(5));
        profileCard.add(memberLabel);

        // Details card
        JPanel detailsCard = new JPanel();
        detailsCard.setLayout(new BoxLayout(detailsCard, BoxLayout.Y_AXIS));
        detailsCard.setBackground(Color.WHITE);
        detailsCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel detailsTitle = new JLabel("My Profile");
        detailsTitle.setFont(new Font("Arial", Font.BOLD, 21));
        detailsTitle.setForeground(darkGreen);
        detailsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        nameField = new JTextField();
        emailField = new JTextField();
        phoneField = new JTextField();

        addField(detailsCard, "Full Name", nameField);
        addField(detailsCard, "Email Address", emailField);
        addField(detailsCard, "Phone Number", phoneField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        editButton = new JButton("Edit Profile");
        stylePrimaryButton(editButton);
        editButton.addActionListener(e -> enableEditing());

        saveButton = new JButton("Save Changes");
        stylePrimaryButton(saveButton);
        saveButton.setVisible(false);
        saveButton.addActionListener(e -> saveProfile());

        JButton backButton = new JButton("Back to Dashboard");
        styleSecondaryButton(backButton);
        backButton.addActionListener(e -> goDashboard());

        buttons.add(editButton);
        buttons.add(saveButton);
        buttons.add(backButton);

        message = new JLabel(" ");
        message.setForeground(green);
        message.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailsCard.add(detailsTitle);
        detailsCard.add(Box.createVerticalStrut(18));
        detailsCard.add(buttons);
        detailsCard.add(Box.createVerticalStrut(8));
        detailsCard.add(message);

        content.add(profileCard);
        content.add(Box.createVerticalStrut(15));
        content.add(detailsCard);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(background);
        root.add(scrollPane, BorderLayout.CENTER);

        // Bottom navigation
        JPanel nav = new JPanel(new GridLayout(1, 5, 4, 0));
        nav.setBackground(Color.WHITE);
        nav.setBorder(new EmptyBorder(10, 0, 10, 0));

        addNavButton(nav, "⌂\nHome", () -> goDashboard());
        addNavButton(nav, "▤\nHistory",
                () -> openFrame(new TransactionHistoryFrame(token)));
        addNavButton(nav, "+\nAdd", () ->
                JOptionPane.showMessageDialog(this,
                        "Connect your Add Transaction screen here."));
        addNavButton(nav, "▥\nInsights", () ->
                openFrame(new AnalyticsFrame(token)));
        addNavButton(nav, "◉\nBudgets", () ->
                openFrame(new BudgetsFrame(token)));

        root.add(nav, BorderLayout.SOUTH);
    }

    private void addField(JPanel panel, String labelText,
                          JTextField field) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 7));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(102, 102, 102));
        label.setFont(new Font("Arial", Font.PLAIN, 13));

        field.setFont(new Font("Arial", Font.PLAIN, 15));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(221, 221, 221)),
                new EmptyBorder(8, 10, 8, 10)));

        wrapper.add(label, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);

        panel.add(wrapper);
        panel.add(Box.createVerticalStrut(12));
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(darkGreen);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(paleGreen);
        button.setForeground(darkGreen);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 12, 9, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void addNavButton(JPanel nav, String text, Runnable action) {
        JButton button = new JButton(
                "<html><center>" + text.replace("\n", "<br>") + "</center></html>");
        button.setForeground(green);
        button.setBackground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 12));
        button.addActionListener(e -> action.run());
        nav.add(button);
    }

    private void setFieldsEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        phoneField.setEnabled(enabled);

        nameField.setBackground(enabled ? Color.WHITE : new Color(245, 245, 242));
        emailField.setBackground(enabled ? Color.WHITE : new Color(245, 245, 242));
        phoneField.setBackground(enabled ? Color.WHITE : new Color(245, 245, 242));
    }

    private void enableEditing() {
        editing = true;
        setFieldsEnabled(true);
        editButton.setVisible(false);
        saveButton.setVisible(true);
        message.setText(" ");
        nameField.requestFocusInWindow();
    }

    private void updateDisplay() {
        String name = nameField.getText().trim();

        displayName.setText(name.isEmpty() ? "Your Name" : name);
        avatar.setText(name.isEmpty()
                ? "U"
                : name.substring(0, 1).toUpperCase());
    }

    /*
     * Loads profile details from the backend.
     * Expected endpoint: GET /profile
     */
    private void loadProfile() {
        if (token == null || token.trim().isEmpty()) {
            message.setText("Login token missing. Please log in again.");
            return;
        }

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                HttpURLConnection connection = openConnection(
                        "/profile", "GET");

                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String response = readStream(stream);
                connection.disconnect();

                if (status < 200 || status >= 300) {
                    throw new IOException("Server returned HTTP " + status);
                }

                return response;
            }

            @Override
            protected void done() {
                try {
                    String response = get();

                    // Parse JSON with Gson if available in the project.
                    com.google.gson.JsonObject profile =
                            com.google.gson.JsonParser.parseString(response)
                                    .getAsJsonObject();

                    nameField.setText(getJsonValue(profile, "name"));
                    emailField.setText(getJsonValue(profile, "email"));
                    phoneField.setText(getJsonValue(profile, "phone"));

                    updateDisplay();
                    setFieldsEnabled(false);
                    message.setText("Profile loaded.");
                } catch (Exception ex) {
                    message.setText(
                            "Could not load profile. Check /profile API.");
                }
            }
        }.execute();
    }

    private String getJsonValue(com.google.gson.JsonObject object,
                                String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    /*
     * Saves profile details to the backend.
     * Expected endpoint: PUT /profile
     * Expected JSON: {"name":"...","email":"...","phone":"..."}
     */
    private void saveProfile() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter your name.");
            nameField.requestFocusInWindow();
            return;
        }

        if (!email.isEmpty() &&
                !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid email address.");
            emailField.requestFocusInWindow();
            return;
        }

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String json = "{"
                        + "\"name\":\"" + escapeJson(name) + "\","
                        + "\"email\":\"" + escapeJson(email) + "\","
                        + "\"phone\":\"" + escapeJson(phone) + "\""
                        + "}";

                HttpURLConnection connection = openConnection(
                        "/profile", "PUT");
                connection.setDoOutput(true);
                connection.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8");

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String response = readStream(stream);
                connection.disconnect();

                if (status < 200 || status >= 300) {
                    throw new IOException("Server returned HTTP " + status
                            + (response.isEmpty() ? "" : ": " + response));
                }

                return response;
            }

            @Override
            protected void done() {
                try {
                    get();

                    updateDisplay();
                    editing = false;
                    setFieldsEnabled(false);
                    editButton.setVisible(true);
                    saveButton.setVisible(false);
                    message.setText("Profile saved successfully!");
                } catch (Exception ex) {
                    message.setText(
                            "Save failed. Check your backend /profile API.");
                    JOptionPane.showMessageDialog(ProfileFrame.this,
                            "Unable to save profile.\n" + ex.getMessage(),
                            "Profile Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private HttpURLConnection openConnection(String path, String method)
            throws IOException {
        URL url = new URL("http://localhost:8080" + path);
        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Accept", "application/json");

        if (token != null && !token.trim().isEmpty()) {
            connection.setRequestProperty(
                    "Authorization", "Bearer " + token);
        }

        return connection;
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void openFrame(JFrame frame) {
        frame.setVisible(true);
        dispose();
    }

    private void goDashboard() {
        // Update this if your DashboardFrame constructor is different.
        new DashboardFrame(token).setVisible(true);
        dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String token = JOptionPane.showInputDialog(
                    null, "Enter login token:");

            if (token != null && !token.trim().isEmpty()) {
                new ProfileFrame(token.trim()).setVisible(true);
            }
        });
    }
}

package spendly;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color GREEN = new Color(20, 83, 45);
    private static final Color DARK_GREEN = new Color(11, 61, 32);
    private static final Color ACTIVE_GREEN = new Color(8, 47, 24);
    private static final Color BACKGROUND = new Color(243, 245, 250);
    private static final Color TITLE_COLOR = new Color(38, 59, 104);
    private static final Color MUTED = new Color(119, 119, 119);
    private static final Color BORDER_COLOR = new Color(221, 221, 221);

    private static final String API_URL = "http://localhost:8080";

    private JTextField emailField;
    private JPasswordField passwordField;
    private JLabel messageLabel;
    private JButton loginButton;

    public LoginFrame() {
        setTitle("Login | Expense Tracker");
        setSize(520, 640);
        setMinimumSize(new Dimension(500, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        getContentPane().setBackground(BACKGROUND);
        setLayout(new GridBagLayout());

        add(createLoginPanel());
        setLocationRelativeTo(null);
    }

    // ================= LOGIN PANEL =================

    private JPanel createLoginPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(235, 235, 235), 1, true),
                new EmptyBorder(30, 30, 30, 30)));

        panel.setPreferredSize(new Dimension(480, 560));

        // HEADING
        JLabel title = new JLabel("Welcome to Spendly!");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(TITLE_COLOR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setMaximumSize(new Dimension(400, 35));

     // SUBTITLE
        JLabel subtitle = new JLabel(
            "<html><div style='text-align:center;'>"
            + "Track smarter. Spend better.<br>"
            + "Login to manage your expenses."
            + "</div></html>"
        );

        subtitle.setFont(new Font("Arial", Font.ITALIC, 16));
        subtitle.setForeground(new Color(100, 125, 108));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setMaximumSize(new Dimension(400, 45));
        subtitle.setPreferredSize(new Dimension(400, 45));
        // EMAIL
        JLabel emailLabel = new JLabel("Email Address");
        styleLabel(emailLabel);

        emailField = new PlaceholderTextField("Enter your email ID");
        styleInput(emailField);
        emailField.setToolTipText("Enter your registered email address");
        addFocusEffect(emailField);

        // PASSWORD
        JLabel passwordLabel = new JLabel("Password");
        styleLabel(passwordLabel);

        passwordField = new PlaceholderPasswordField("Enter your password");
        styleInput(passwordField);
        passwordField.setToolTipText("Enter your password");
        addFocusEffect(passwordField);

        // LOGIN BUTTON
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 15));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(GREEN);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setOpaque(true);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setPreferredSize(new Dimension(380, 46));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (loginButton.isEnabled()) {
                    loginButton.setBackground(DARK_GREEN);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (loginButton.isEnabled()) {
                    loginButton.setBackground(GREEN);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (loginButton.isEnabled()) {
                    loginButton.setBackground(ACTIVE_GREEN);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (loginButton.isEnabled()) {
                    loginButton.setBackground(DARK_GREEN);
                }
            }
        });

        // STATUS MESSAGE
        messageLabel = new JLabel(" ");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        messageLabel.setForeground(MUTED);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // SIGNUP LINK
        JLabel signupLabel = new JLabel(
                "<html>Don't have an account? "
                + "<font color='#14532D'><b>Sign Up</b></font></html>");

        signupLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        signupLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signupLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        signupLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openSignup();
            }
        });

        loginButton.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());

        // ADD COMPONENTS
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(28));

        panel.add(emailLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(emailField);
        panel.add(Box.createVerticalStrut(16));

        panel.add(passwordLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(20));

        panel.add(loginButton);
        panel.add(Box.createVerticalStrut(12));

        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(18));

        panel.add(signupLabel);

        return panel;
    }

    // ================= LABEL STYLE =================

    private void styleLabel(JLabel label) {
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setForeground(TITLE_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
    }

    // ================= INPUT STYLE =================

    private void styleInput(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 15));
        field.setForeground(new Color(26, 28, 27));
        field.setBackground(Color.WHITE);

        // Wider and taller input boxes
        Dimension inputSize = new Dimension(380, 46);

        field.setPreferredSize(inputSize);
        field.setMinimumSize(inputSize);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
    }

    // ================= INPUT FOCUS EFFECT =================

    private void addFocusEffect(JTextField field) {
        field.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(new CompoundBorder(
                        new LineBorder(GREEN, 2, true),
                        new EmptyBorder(9, 11, 9, 11)));
                field.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(new CompoundBorder(
                        new LineBorder(BORDER_COLOR, 1, true),
                        new EmptyBorder(10, 12, 10, 12)));
                field.repaint();
            }
        });
    }

    // ================= LOGIN REQUEST =================

    private void login() {

        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            showMessage(
                    "Please enter email and password.",
                    new Color(185, 28, 28));
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setBackground(GREEN);
        showMessage("Checking your account...", MUTED);

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {

                HttpURLConnection connection = (HttpURLConnection)
                        new URL(API_URL + "/login").openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8");

                String form =
                        "email=" + URLEncoder.encode(email, "UTF-8")
                        + "&password=" + URLEncoder.encode(password, "UTF-8");

                try {
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(form.getBytes(StandardCharsets.UTF_8));
                    }

                    int status = connection.getResponseCode();

                    InputStream input =
                            status >= 200 && status < 400
                            ? connection.getInputStream()
                            : connection.getErrorStream();

                    StringBuilder response = new StringBuilder();

                    if (input != null) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(
                                        input, StandardCharsets.UTF_8))) {

                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                        }
                    }

                    String json = response.toString();

                    if (status < 200 || status >= 300) {
                        String error = getJsonValue(json, "message");

                        if (error == null) {
                            error = "Login failed. HTTP " + status;
                        }

                        throw new IOException(error);
                    }

                    return json;

                } finally {
                    connection.disconnect();
                }
            }

            @Override
            protected void done() {
                try {
                    String json = get();

                    String userId = getJsonValue(json, "userId");
                    String token = getJsonValue(json, "token");

                    if (userId == null || token == null || token.trim().isEmpty()) {
                        showMessage(
                                "Invalid login response from server.",
                                new Color(185, 28, 28));
                        return;
                    }

                    showMessage("Login successful!", GREEN);

                    DashboardFrame dashboard = new DashboardFrame(token);
                    dashboard.setLocationRelativeTo(null);
                    dashboard.setVisible(true);

                    dispose();

                } catch (Exception ex) {
                    Throwable cause = ex.getCause();

                    String error = cause != null
                            ? cause.getMessage()
                            : ex.getMessage();

                    if (error == null || error.trim().isEmpty()) {
                        error = "Unable to log in. Check your server.";
                    }

                    showMessage(error, new Color(185, 28, 28));

                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setBackground(GREEN);
                }
            }
        };

        worker.execute();
    }

    // ================= READ JSON VALUE =================

    private String getJsonValue(String json, String key) {

        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key)
                + "\"\\s*:\\s*"
                + "(?:\"((?:\\\\.|[^\"])*)\"|(-?[0-9]+))");

        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1) != null
                    ? matcher.group(1)
                    : matcher.group(2);
        }

        return null;
    }

    // ================= STATUS MESSAGE =================

    private void showMessage(String text, Color color) {
        messageLabel.setText(
                "<html><div style='width:310px;text-align:center'>"
                + escapeHtml(text)
                + "</div></html>");

        messageLabel.setForeground(color);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    // ================= SIGNUP NAVIGATION =================

    private void openSignup() {
        try {
            Class<?> clazz = Class.forName("spendly.SignupFrame");

            JFrame signup = (JFrame) clazz
                    .getDeclaredConstructor()
                    .newInstance();

            signup.setLocationRelativeTo(this);
            signup.setVisible(true);
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "SignupFrame is not ready yet.",
                    "Spendly",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ================= EMAIL PLACEHOLDER =================

    private static class PlaceholderTextField extends JTextField {

        private static final long serialVersionUID = 1L;
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(160, 160, 160));
                g2.setFont(getFont());

                Insets insets = getInsets();
                int y = (getHeight()
                        - g2.getFontMetrics().getHeight()) / 2
                        + g2.getFontMetrics().getAscent();

                g2.drawString(placeholder, insets.left + 2, y);
                g2.dispose();
            }
        }
    }

    // ================= PASSWORD PLACEHOLDER =================

    private static class PlaceholderPasswordField extends JPasswordField {

        private static final long serialVersionUID = 1L;
        private final String placeholder;

        PlaceholderPasswordField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(160, 160, 160));
                g2.setFont(getFont());

                Insets insets = getInsets();
                int y = (getHeight()
                        - g2.getFontMetrics().getHeight()) / 2
                        + g2.getFontMetrics().getAscent();

                g2.drawString(placeholder, insets.left + 2, y);
                g2.dispose();
            }
        }
    }

    // ================= MAIN =================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}

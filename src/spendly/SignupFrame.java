
package spendly;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.UnsupportedEncodingException;

public class SignupFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();

    private final JButton signupButton = new JButton("Create Account");
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);

    private final Color green = new Color(34, 164, 71);
    private final Color darkGreen = new Color(24, 134, 56);
    private final Color navy = new Color(38, 59, 104);

    public SignupFrame() {
        setTitle("Create Account | Spendly");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 650);
        setMinimumSize(new Dimension(420, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(243, 245, 250));

        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(new Color(243, 245, 250));
        outer.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(28, 30, 25, 30));
        card.setPreferredSize(new Dimension(390, 550));

        JLabel heading = new JLabel("Create Account");
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        heading.setForeground(navy);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Start managing your money smarter.");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(new Color(119, 119, 119));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(heading);
        card.add(Box.createVerticalStrut(10));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(25));

        addField(card, "Full Name", nameField);
        addField(card, "Email Address", emailField);
        addField(card, "Password (minimum 8 characters)", passwordField);
        addField(card, "Confirm Password", confirmPasswordField);

        signupButton.setFont(new Font("Arial", Font.BOLD, 15));
        signupButton.setForeground(Color.WHITE);
        signupButton.setBackground(green);
        signupButton.setOpaque(true);
        signupButton.setBorderPainted(false);
        signupButton.setFocusPainted(false);
        signupButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signupButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        signupButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        signupButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (signupButton.isEnabled()) {
                    signupButton.setBackground(darkGreen);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                signupButton.setBackground(green);
            }
        });

        signupButton.addActionListener(e -> createAccount());

        card.add(Box.createVerticalStrut(12));
        card.add(signupButton);
        card.add(Box.createVerticalStrut(12));

        messageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setMaximumSize(new Dimension(330, 45));
        card.add(messageLabel);

        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 5));
        loginPanel.setBackground(Color.WHITE);

        JLabel already = new JLabel("Already have an account?");
        already.setForeground(new Color(100, 100, 100));

        JButton loginLink = new JButton("Login");
        loginLink.setFont(new Font("Arial", Font.PLAIN, 14));
        loginLink.setForeground(new Color(76, 101, 217));
        loginLink.setBorderPainted(false);
        loginLink.setContentAreaFilled(false);
        loginLink.setFocusPainted(false);
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        loginLink.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        loginPanel.add(already);
        loginPanel.add(loginLink);
        card.add(loginPanel);

        outer.add(card);
        add(outer);
    }

    private void addField(JPanel panel, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.PLAIN, 13));
        label.setForeground(new Color(75, 75, 75));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(221, 221, 221)),
            new EmptyBorder(10, 12, 10, 12)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setPreferredSize(new Dimension(320, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMinimumSize(new Dimension(100, 44));

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(green),
                    new EmptyBorder(10, 12, 10, 12)
                ));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(221, 221, 221)),
                    new EmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        panel.add(Box.createVerticalStrut(12));
    }

    private void createAccount() {
        final String name = nameField.getText().trim();
        final String email = emailField.getText().trim();
        final String password = new String(passwordField.getPassword());
        String confirmPassword =
                new String(confirmPasswordField.getPassword());

        if (name.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Please fill in all fields.", Color.RED);
            return;
        }

        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            showMessage("Please enter a valid email address.", Color.RED);
            return;
        }

        if (password.length() < 8) {
            showMessage(
                "Password must contain at least 8 characters.",
                Color.RED
            );
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Passwords do not match!", Color.RED);
            return;
        }

        signupButton.setEnabled(false);
        signupButton.setText("Please wait...");
        showMessage("Creating your account...", Color.GRAY);

        new SwingWorker<String, Void>() {
            private int statusCode;

            @Override
            protected String doInBackground() throws Exception {
                String body = "name=" + encode(name)
                    + "&email=" + encode(email)
                    + "&password=" + encode(password);

                HttpURLConnection connection = null;

                try {
                    URL url = new URL("http://localhost:8080/signup");
                    connection =
                        (HttpURLConnection) url.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                    );

                    OutputStream out = connection.getOutputStream();
                    try {
                        out.write(body.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    } finally {
                        out.close();
                    }

                    statusCode = connection.getResponseCode();

                    InputStream input =
                        (statusCode >= 200 && statusCode < 400)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                    if (input == null) {
                        return "Server returned HTTP " + statusCode;
                    }

                    try {
                        ByteArrayOutputStream buffer =
                            new ByteArrayOutputStream();

                        byte[] data = new byte[1024];
                        int count;

                        while ((count = input.read(data)) != -1) {
                            buffer.write(data, 0, count);
                        }

                        return new String(
                            buffer.toByteArray(),
                            StandardCharsets.UTF_8
                        );
                    } finally {
                        input.close();
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    String responseBody = get();
                    String resultMessage = extractMessage(responseBody);

                    if (statusCode >= 200 && statusCode < 300) {
                        showMessage(
                            resultMessage,
                            new Color(24, 134, 56)
                        );
                        nameField.setText("");
                        emailField.setText("");
                        passwordField.setText("");
                        confirmPasswordField.setText("");
                    } else {
                        showMessage(resultMessage, Color.RED);
                    }
                } catch (Exception ex) {
                    showMessage(
                        "Server connection failed. Check Java server.",
                        Color.RED
                    );
                } finally {
                    signupButton.setEnabled(true);
                    signupButton.setText("Create Account");
                }
            }
        }.execute();
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractMessage(String json) {
        Pattern pattern = Pattern.compile(
            "\"(?:message|error)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        );

        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        }

        return json.trim().isEmpty()
            ? "Request completed."
            : json;
    }

    private void showMessage(String message, Color color) {
        messageLabel.setText(
            "<html><div style='text-align:center;width:300px;'>"
            + escapeHtml(message) + "</div></html>"
        );
        messageLabel.setForeground(color);
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
            () -> new SignupFrame().setVisible(true)
        );
    }
}

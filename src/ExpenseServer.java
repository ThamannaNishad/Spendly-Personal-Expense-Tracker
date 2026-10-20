import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.sql.Statement;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExpenseServer {

    private static final String DB_URL =
        "jdbc:mysql://localhost:3306/expense_tracker";

    private static final String DB_USER = "root";
 // Database password
    private static final String DB_PASSWORD =
            System.getenv("EXPENSE_DB_PASSWORD");
    private static final Map<String, Integer> sessions =
        new ConcurrentHashMap<String, Integer>();

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(8080)) {
            System.out.println(
                "ExpenseServer running at http://localhost:8080"
            );

            while (true) {
                final Socket client = server.accept();

                Thread worker = new Thread(() -> handleClient(client));
                worker.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        if (DB_PASSWORD == null || DB_PASSWORD.isEmpty()) {
            throw new IllegalStateException(
                "Set the EXPENSE_DB_PASSWORD environment variable."
            );
        }

        return DriverManager.getConnection(
            DB_URL, DB_USER, DB_PASSWORD
        );
    }

    private static void handleClient(Socket client) {
        try (
            Socket socket = client;
            BufferedInputStream in =
                new BufferedInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream()
        ) {
            String requestLine = readLine(in);

            if (requestLine == null || requestLine.trim().isEmpty()) {
                return;
            }

            String[] requestParts = requestLine.split(" ");

            if (requestParts.length < 2) {
                respondJson(out, 400, message("Invalid request."));
                return;
            }

            String method =
                requestParts[0].toUpperCase(Locale.ROOT);

            String path = requestParts[1].split("\\?", 2)[0];

            Map<String, String> headers =
                new HashMap<String, String>();

            String headerLine;

            while ((headerLine = readLine(in)) != null
                    && !headerLine.isEmpty()) {

                int colon = headerLine.indexOf(':');

                if (colon > 0) {
                    String key = headerLine.substring(0, colon)
                        .trim().toLowerCase(Locale.ROOT);

                    String value =
                        headerLine.substring(colon + 1).trim();

                    headers.put(key, value);
                }
            }

            System.out.println(method + " " + path);

            if ("OPTIONS".equals(method)) {
                respond(out, 204, "");
                return;
            }

            int contentLength;

            try {
                contentLength = Integer.parseInt(
                    headers.getOrDefault("content-length", "0")
                );
            } catch (NumberFormatException e) {
                respondJson(out, 400, message("Invalid content length."));
                return;
            }

            if (contentLength < 0 || contentLength > 1_000_000) {
                respondJson(out, 400, message("Invalid request size."));
                return;
            }

            byte[] bodyBytes = readBytes(in, contentLength);

            if (bodyBytes.length != contentLength) {
                respondJson(out, 400, message("Incomplete request body."));
                return;
            }

            String body = new String(
                bodyBytes, StandardCharsets.UTF_8
            );

            Map<String, String> form = parseForm(body);
            JsonObject jsonBody = parseJsonObject(body);

            String authorization = headers.get("authorization");

            if ("POST".equals(method) && "/signup".equals(path)) {
                handleSignup(out, form);
                return;
            }

            if ("POST".equals(method) && "/login".equals(path)) {
                handleLogin(out, form);
                return;
            }

            if ("POST".equals(method) && "/logout".equals(path)) {
                String token = getToken(authorization);

                if (token != null) {
                    sessions.remove(token);
                }

                respondJson(out, 200, message("Logged out successfully."));
                return;
            }

            Integer userId = authenticate(authorization);

            if (userId == null) {
                respondJson(out, 401, message("Please log in first."));
                return;
            }

            if ("GET".equals(method) && "/summary".equals(path)) {
                handleSummary(out, userId);
                return;
            }

            if ("GET".equals(method) && "/transactions".equals(path)) {
                handleTransactions(out, userId);
                return;
            }

            if ("POST".equals(method) && "/save".equals(path)) {
                handleSaveTransaction(out, userId, form, jsonBody);
                return;
            }

            if ("GET".equals(method) && "/budgets".equals(path)) {
                handleGetBudgets(out, userId);
                return;
            }

            if ("PUT".equals(method) && "/budgets".equals(path)) {
                handlePutBudgets(out, userId, jsonBody);
                return;
            }

            if ("GET".equals(method) && "/goals".equals(path)) {
                handleGetGoals(out, userId);
                return;
            }

            if ("POST".equals(method) && "/goals".equals(path)) {
                handleCreateGoal(out, userId, jsonBody);
                return;
            }

            if (path.matches("/goals/\\d+")) {
                int goalId = Integer.parseInt(
                    path.substring(path.lastIndexOf('/') + 1)
                );

                if ("PUT".equals(method)) {
                    handleUpdateGoal(out, userId, goalId, jsonBody);
                    return;
                }

                if ("DELETE".equals(method)) {
                    handleDeleteGoal(out, userId, goalId);
                    return;
                }
            }

            if ("GET".equals(method) && "/profile".equals(path)) {
                handleGetProfile(out, userId);
                return;
            }

            if ("PUT".equals(method) && "/profile".equals(path)) {
                handleUpdateProfile(out, userId, jsonBody);
                return;
            }

            respondJson(out, 404, message("Endpoint not found."));

        } catch (Exception e) {
            e.printStackTrace();

            try {
                respondJson(
                    client.getOutputStream(),
                    500,
                    message("Internal server error.")
                );
            } catch (Exception ignored) {
            }

        } finally {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static String readLine(InputStream in) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int current;
        int previous = -1;

        while ((current = in.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                byte[] bytes = buffer.toByteArray();

                return new String(
                    bytes, 0, bytes.length - 1,
                    StandardCharsets.UTF_8
                );
            }

            buffer.write(current);
            previous = current;
        }

        if (buffer.size() == 0) {
            return null;
        }

        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(InputStream in, int length)
            throws Exception {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int remaining = length;

        while (remaining > 0) {
            int count = in.read(
                chunk, 0, Math.min(chunk.length, remaining)
            );

            if (count == -1) {
                break;
            }

            buffer.write(chunk, 0, count);
            remaining -= count;
        }

        return buffer.toByteArray();
    }
    private static void handleSignup(
            OutputStream out, Map<String, String> data)
            throws Exception {

        String name = data.get("name");
        String email = data.get("email");
        String password = data.get("password");

        if (name == null || name.trim().isEmpty()
                || email == null
                || !Pattern.matches(
                    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                    email.trim())
                || password == null || password.length() < 8) {

            respondJson(out, 400, message(
                "Enter a valid name, email and password of at least 8 characters."
            ));
            return;
        }

        String sql =
            "INSERT INTO users (name,email,password_hash) VALUES (?,?,?)";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, name.trim());
            ps.setString(2, email.trim().toLowerCase(Locale.ROOT));
            ps.setString(3, hashPassword(password));
            ps.executeUpdate();

            respondJson(out, 200, message(
                "Account created successfully! You can now log in."
            ));

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                respondJson(out, 409, message(
                    "This email is already registered."
                ));
            } else {
                throw e;
            }
        }
    }

    private static void handleLogin(
            OutputStream out, Map<String, String> data)
            throws Exception {

        String email = data.get("email");
        String password = data.get("password");

        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {
            respondJson(out, 400, message(
                "Email and password are required."
            ));
            return;
        }

        String sql =
            "SELECT id,name,password_hash FROM users WHERE email=?";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, email.trim().toLowerCase(Locale.ROOT));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()
                        || !verifyPassword(
                            password, rs.getString("password_hash"))) {
                    respondJson(out, 401, message(
                        "Invalid email or password."
                    ));
                    return;
                }

                int id = rs.getInt("id");
                String token = UUID.randomUUID().toString();

                sessions.put(token, id);

                JsonObject response = new JsonObject();
                response.addProperty("message", "Login successful!");
                response.addProperty("userId", id);
                response.addProperty("name", rs.getString("name"));
                response.addProperty("token", token);

                respondJson(out, 200, response.toString());
            }
        }
    }

    private static void handleSummary(
            OutputStream out, int userId) throws Exception {

        String sql =
            "SELECT " +
            "COALESCE(SUM(CASE WHEN LOWER(type)='income' " +
            "THEN amount ELSE 0 END),0) AS income, " +
            "COALESCE(SUM(CASE WHEN LOWER(type)='expense' " +
            "THEN amount ELSE 0 END),0) AS expenses " +
            "FROM transactions WHERE user_id=?";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();

                BigDecimal income = rs.getBigDecimal("income");
                BigDecimal expenses = rs.getBigDecimal("expenses");

                JsonObject result = new JsonObject();
                result.addProperty("income", income);
                result.addProperty("expenses", expenses);
                result.addProperty("balance", income.subtract(expenses));

                respondJson(out, 200, result.toString());
            }
        }
    }

    private static void handleTransactions(
            OutputStream out, int userId) throws Exception {

        String sql =
            "SELECT id,title,amount,category,type,transaction_date,notes " +
            "FROM transactions WHERE user_id=? ORDER BY id DESC";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                JsonArray result = new JsonArray();

                while (rs.next()) {
                    JsonObject item = new JsonObject();

                    item.addProperty("id", rs.getInt("id"));
                    item.addProperty("title", rs.getString("title"));
                    item.addProperty("amount", rs.getBigDecimal("amount"));
                    item.addProperty("category", rs.getString("category"));
                    item.addProperty("type", rs.getString("type"));

                    java.sql.Date date = rs.getDate("transaction_date");

                    if (date != null) {
                        item.addProperty("date", date.toString());
                    }

                    item.addProperty("notes", rs.getString("notes"));
                    result.add(item);
                }

                respondJson(out, 200, result.toString());
            }
        }
    }

    private static void handleSaveTransaction(
            OutputStream out,
            int userId,
            Map<String, String> form,
            JsonObject json) throws Exception {

        String title = value(form, json, "title");
        String amountText = value(form, json, "amount");
        String category = value(form, json, "category");
        String type = value(form, json, "type");
        String dateText = value(form, json, "date");
        String notes = value(form, json, "notes");

        // Notes/description optional aanu.
        if (title == null || title.trim().isEmpty()
                || amountText == null || amountText.trim().isEmpty()
                || category == null || category.trim().isEmpty()) {
            respondJson(out, 400, message(
                "Title, amount and category are required."
            ));
            return;
        }

        if (type == null || type.trim().isEmpty()) {
            type = "Expense";
        }

        if (!"Expense".equalsIgnoreCase(type)
                && !"Income".equalsIgnoreCase(type)) {
            respondJson(out, 400, message("Invalid transaction type."));
            return;
        }

        BigDecimal amount;

        try {
            amount = new BigDecimal(amountText.trim());
        } catch (NumberFormatException e) {
            respondJson(out, 400, message("Invalid amount."));
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            respondJson(out, 400, message(
                "Amount must be greater than zero."
            ));
            return;
        }

        String sql =
            "INSERT INTO transactions " +
            "(title,amount,category,type,transaction_date,notes,user_id) " +
            "VALUES (?,?,?,?,?,?,?)";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, title.trim());
            ps.setBigDecimal(2, amount);
            ps.setString(3, category.trim());
            ps.setString(4, type);

            if (dateText == null || dateText.trim().isEmpty()) {
                ps.setDate(
                    5, new java.sql.Date(System.currentTimeMillis())
                );
            } else {
                ps.setDate(5, java.sql.Date.valueOf(dateText.trim()));
            }

            ps.setString(6, notes == null ? "" : notes.trim());
            ps.setInt(7, userId);
            ps.executeUpdate();
        }

        respondJson(out, 200, message("Transaction saved successfully!"));
    }

    private static void handleGetBudgets(
            OutputStream out, int userId) throws Exception {

        String sql =
            "SELECT monthly_budget,category_limits " +
            "FROM budgets WHERE user_id=?";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                JsonObject result = new JsonObject();

                if (rs.next()) {
                    BigDecimal monthlyBudget =
                        rs.getBigDecimal("monthly_budget");

                    result.addProperty(
                        "monthlyBudget",
                        monthlyBudget == null
                            ? BigDecimal.ZERO : monthlyBudget
                    );

                    String limits = rs.getString("category_limits");

                    if (limits == null || limits.trim().isEmpty()) {
                        result.add("limits", new JsonObject());
                    } else {
                        JsonElement parsed = JsonParser.parseString(limits);

                        result.add(
                            "limits",
                            parsed.isJsonObject()
                                ? parsed.getAsJsonObject()
                                : new JsonObject()
                        );
                    }
                } else {
                    result.addProperty("monthlyBudget", 8000);
                    result.add("limits", new JsonObject());
                }

                respondJson(out, 200, result.toString());
            }
        }
    }

    private static void handlePutBudgets(
            OutputStream out,
            int userId,
            JsonObject body) throws Exception {

        if (body == null) {
            respondJson(out, 400, message("Invalid JSON request."));
            return;
        }

        BigDecimal monthlyBudget = new BigDecimal("8000");

        try {
            if (body.has("monthlyBudget")
                    && !body.get("monthlyBudget").isJsonNull()) {
                monthlyBudget =
                    body.get("monthlyBudget").getAsBigDecimal();
            }
        } catch (Exception e) {
            respondJson(out, 400, message("Invalid monthly budget."));
            return;
        }

        if (monthlyBudget.compareTo(BigDecimal.ZERO) < 0) {
            respondJson(out, 400, message(
                "Monthly budget cannot be negative."
            ));
            return;
        }

        JsonObject limits = new JsonObject();

        if (body.has("limits") && body.get("limits").isJsonObject()) {
            limits = body.getAsJsonObject("limits");
        }

        String sql =
            "INSERT INTO budgets " +
            "(user_id,monthly_budget,category_limits) VALUES (?,?,?) " +
            "ON DUPLICATE KEY UPDATE " +
            "monthly_budget=VALUES(monthly_budget), " +
            "category_limits=VALUES(category_limits)";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);
            ps.setBigDecimal(2, monthlyBudget);
            ps.setString(3, limits.toString());
            ps.executeUpdate();
        }

        respondJson(out, 200, message("Budgets saved successfully."));
    }
    private static void handleGetGoals(
            OutputStream out, int userId) throws Exception {

        String sql =
            "SELECT id,name,target,saved FROM savings_goals " +
            "WHERE user_id=? ORDER BY id DESC";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                JsonArray result = new JsonArray();

                while (rs.next()) {
                    JsonObject goal = new JsonObject();
                    goal.addProperty("id", rs.getInt("id"));
                    goal.addProperty("name", rs.getString("name"));
                    goal.addProperty("target", rs.getBigDecimal("target"));
                    goal.addProperty("saved", rs.getBigDecimal("saved"));
                    result.add(goal);
                }

                respondJson(out, 200, result.toString());
            }
        }
    }

    private static void handleCreateGoal(
            OutputStream out, int userId, JsonObject body)
            throws Exception {

        if (body == null) {
            respondJson(out, 400, message("Invalid JSON request."));
            return;
        }

        String name = jsonString(body, "name", "").trim();
        BigDecimal target;

        try {
            if (!body.has("target") || body.get("target").isJsonNull()) {
                throw new NumberFormatException();
            }

            target = body.get("target").getAsBigDecimal();

        } catch (Exception e) {
            respondJson(out, 400, message("Enter a valid target amount."));
            return;
        }

        if (name.isEmpty() || target.compareTo(BigDecimal.ZERO) <= 0) {
            respondJson(out, 400, message(
                "Goal name and a target greater than zero are required."
            ));
            return;
        }

        String sql =
            "INSERT INTO savings_goals (user_id,name,target,saved) " +
            "VALUES (?,?,?,0)";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
            )
        ) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setBigDecimal(3, target);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Could not get new goal ID.");
                }

                JsonObject goal = new JsonObject();
                goal.addProperty("id", keys.getInt(1));
                goal.addProperty("name", name);
                goal.addProperty("target", target);
                goal.addProperty("saved", BigDecimal.ZERO);

                respondJson(out, 201, goal.toString());
            }
        }
    }

    private static void handleUpdateGoal(
            OutputStream out,
            int userId,
            int goalId,
            JsonObject body) throws Exception {

        if (body == null || !body.has("saved")
                || body.get("saved").isJsonNull()) {
            respondJson(out, 400, message("A saved amount is required."));
            return;
        }

        BigDecimal saved;

        try {
            saved = body.get("saved").getAsBigDecimal();
        } catch (Exception e) {
            respondJson(out, 400, message("Invalid saved amount."));
            return;
        }

        if (saved.compareTo(BigDecimal.ZERO) < 0) {
            respondJson(out, 400, message(
                "Saved amount cannot be negative."
            ));
            return;
        }

        String sql =
            "UPDATE savings_goals SET saved=? WHERE id=? AND user_id=?";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setBigDecimal(1, saved);
            ps.setInt(2, goalId);
            ps.setInt(3, userId);

            if (ps.executeUpdate() == 0) {
                respondJson(out, 404, message("Goal not found."));
                return;
            }
        }

        String selectSql =
            "SELECT id,name,target,saved FROM savings_goals " +
            "WHERE id=? AND user_id=?";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(selectSql)
        ) {
            ps.setInt(1, goalId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    respondJson(out, 404, message("Goal not found."));
                    return;
                }

                JsonObject goal = new JsonObject();
                goal.addProperty("id", rs.getInt("id"));
                goal.addProperty("name", rs.getString("name"));
                goal.addProperty("target", rs.getBigDecimal("target"));
                goal.addProperty("saved", rs.getBigDecimal("saved"));

                respondJson(out, 200, goal.toString());
            }
        }
    }

    private static void handleDeleteGoal(
            OutputStream out, int userId, int goalId)
            throws Exception {

        String sql =
            "DELETE FROM savings_goals WHERE id=? AND user_id=?";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setInt(1, goalId);
            ps.setInt(2, userId);

            if (ps.executeUpdate() == 0) {
                respondJson(out, 404, message("Goal not found."));
                return;
            }
        }

        respondJson(out, 200, message("Goal deleted successfully."));
    }

    private static void handleGetProfile(
            OutputStream out, int userId) throws Exception {

        String sql = "SELECT id,name,email FROM users WHERE id=?";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    respondJson(out, 404, message("Profile not found."));
                    return;
                }

                JsonObject profile = new JsonObject();
                profile.addProperty("id", rs.getInt("id"));
                profile.addProperty("name", rs.getString("name"));
                profile.addProperty("email", rs.getString("email"));

                respondJson(out, 200, profile.toString());
            }
        }
    }

    private static void handleUpdateProfile(
            OutputStream out, int userId, JsonObject body)
            throws Exception {

        if (body == null) {
            respondJson(out, 400, message("Invalid JSON request."));
            return;
        }

        String name = jsonString(body, "name", "").trim();
        String email = jsonString(body, "email", "")
            .trim().toLowerCase(Locale.ROOT);

        if (name.isEmpty()
                || !Pattern.matches(
                    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                    email)) {
            respondJson(out, 400, message(
                "Enter a valid name and email address."
            ));
            return;
        }

        String sql = "UPDATE users SET name=?,email=? WHERE id=?";

        try (
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setInt(3, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                respondJson(out, 409, message(
                    "That email address is already in use."
                ));
                return;
            }
            throw e;
        }

        JsonObject profile = new JsonObject();
        profile.addProperty("id", userId);
        profile.addProperty("name", name);
        profile.addProperty("email", email);

        respondJson(out, 200, profile.toString());
    }

    private static JsonObject parseJsonObject(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        try {
            JsonElement element = JsonParser.parseString(body);

            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (Exception ignored) {
            // The request may be form-encoded.
        }

        return null;
    }

    private static String value(
            Map<String, String> form,
            JsonObject json,
            String key) {

        if (form != null && form.containsKey(key)) {
            return form.get(key);
        }

        return jsonString(json, key, null);
    }

    private static String jsonString(
            JsonObject object, String key, String fallback) {

        if (object == null || !object.has(key)
                || object.get(key).isJsonNull()) {
            return fallback;
        }

        try {
            return object.get(key).getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Map<String, String> parseForm(String body)
            throws Exception {

        Map<String, String> data = new HashMap<String, String>();

        if (body == null || body.isEmpty()) {
            return data;
        }

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(parts[0], "UTF-8");
            String value = parts.length > 1
                ? URLDecoder.decode(parts[1], "UTF-8")
                : "";

            data.put(key, value);
        }

        return data;
    }

    private static String getToken(String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return null;
        }

        return authorization.substring(7).trim();
    }

    private static Integer authenticate(String authorization) {
        String token = getToken(authorization);

        if (token == null || token.isEmpty()) {
            return null;
        }

        return sessions.get(token);
    }

    private static String hashPassword(String password)
            throws Exception {

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(), salt, 210000, 256
        );

        byte[] hash;

        try {
            hash = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
        } finally {
            spec.clearPassword();
        }

        return Base64.getEncoder().encodeToString(salt)
            + ":"
            + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verifyPassword(
            String password, String storedHash) throws Exception {

        if (storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split(":");

        if (parts.length != 2) {
            return false;
        }

        byte[] salt;
        byte[] expectedHash;

        try {
            salt = Base64.getDecoder().decode(parts[0]);
            expectedHash = Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            return false;
        }

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(), salt, 210000, 256
        );

        byte[] actualHash;

        try {
            actualHash = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
        } finally {
            spec.clearPassword();
        }

        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private static String message(String text) {
        JsonObject object = new JsonObject();
        object.addProperty("message", text);
        return object.toString();
    }

    private static void respondJson(
            OutputStream out, int status, String json)
            throws Exception {

        byte[] body = json.getBytes(StandardCharsets.UTF_8);

        String headers =
            "HTTP/1.1 " + status + " " + statusText(status) + "\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
            "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n" +
            "Content-Length: " + body.length + "\r\n" +
            "Connection: close\r\n\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private static void respond(
            OutputStream out, int status, String text)
            throws Exception {

        byte[] body = text.getBytes(StandardCharsets.UTF_8);

        String headers =
            "HTTP/1.1 " + status + " " + statusText(status) + "\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
            "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
            "Content-Type: text/plain; charset=UTF-8\r\n" +
            "Content-Length: " + body.length + "\r\n" +
            "Connection: close\r\n\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private static String statusText(int status) {
        switch (status) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 404: return "Not Found";
            case 409: return "Conflict";
            case 500: return "Internal Server Error";
            default: return "Error";
        }
    }
}

package com.udaanix.siya;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.*;
import java.util.UUID;

/** STEP 8: Persistent SIYA conversation storage using MySQL/JDBC. */
public class ConversationService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String url;
    private final String user;
    private final String password;

    public ConversationService() {
        String host = env("UDAANIX_DB_HOST", "localhost");
        String port = env("UDAANIX_DB_PORT", "3306");
        String db = env("UDAANIX_DB_NAME", "udaanix");
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        this.user = env("UDAANIX_DB_USER", "root");
        this.password = env("UDAANIX_DB_PASSWORD", "");
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public String createConversation(String studentId, String firstMessage) throws SQLException {
        String id = UUID.randomUUID().toString();
        String title = makeTitle(firstMessage);
        String sql = "INSERT INTO siya_conversations (conversation_id, student_id, title) VALUES (?, ?, ?)";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            if (studentId == null || studentId.isBlank()) ps.setNull(2, Types.VARCHAR); else ps.setString(2, studentId);
            ps.setString(3, title);
            ps.executeUpdate();
        }
        return id;
    }

    public void saveMessage(String conversationId, String sender, String message) throws SQLException {
        String sql = "INSERT INTO siya_messages (conversation_id, sender, message) VALUES (?, ?, ?)";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, sender);
            ps.setString(3, message);
            ps.executeUpdate();
        }
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement("UPDATE siya_conversations SET updated_at=CURRENT_TIMESTAMP WHERE conversation_id=?")) {
            ps.setString(1, conversationId); ps.executeUpdate();
        }
    }

    public boolean conversationBelongsTo(String conversationId, String studentId) throws SQLException {
        if (conversationId == null || conversationId.isBlank()) return false;
        String sql = "SELECT student_id FROM siya_conversations WHERE conversation_id=?";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String owner = rs.getString(1);
                return studentId != null && !studentId.isBlank() && studentId.equals(owner);
            }
        }
    }

    public ArrayNode listConversations(String studentId) throws SQLException {
        ArrayNode out = mapper.createArrayNode();
        String sql = "SELECT conversation_id, title, created_at, updated_at FROM siya_conversations WHERE student_id=? ORDER BY updated_at DESC LIMIT 50";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ObjectNode o = mapper.createObjectNode();
                    o.put("conversationId", rs.getString("conversation_id"));
                    o.put("title", rs.getString("title"));
                    o.put("createdAt", String.valueOf(rs.getTimestamp("created_at")));
                    o.put("updatedAt", String.valueOf(rs.getTimestamp("updated_at")));
                    out.add(o);
                }
            }
        }
        return out;
    }

    public ArrayNode listMessages(String conversationId, String studentId) throws SQLException {
        if (!conversationBelongsTo(conversationId, studentId)) throw new SecurityException("Conversation not found.");
        ArrayNode out = mapper.createArrayNode();
        String sql = "SELECT id, sender, message, created_at FROM siya_messages WHERE conversation_id=? ORDER BY id ASC";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ObjectNode o = mapper.createObjectNode();
                    o.put("id", rs.getLong("id"));
                    o.put("sender", rs.getString("sender"));
                    o.put("message", rs.getString("message"));
                    o.put("createdAt", String.valueOf(rs.getTimestamp("created_at")));
                    out.add(o);
                }
            }
        }
        return out;
    }

    private String makeTitle(String text) {
        if (text == null || text.isBlank()) return "New SIYA Conversation";
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > 55 ? clean.substring(0, 55) + "…" : clean;
    }
}

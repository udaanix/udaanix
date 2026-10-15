package com.udaanix.servlet;

import com.udaanix.config.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Counsellor Notifications Servlet
 *
 * GET:
 *   /api/counsellor/notifications
 *   Returns counsellor notifications + unread count.
 *
 * PUT:
 *   /api/counsellor/notifications?notificationId=10
 *   Marks one notification as read.
 *
 * PUT:
 *   /api/counsellor/notifications?markAllRead=true
 *   Marks all counsellor notifications as read.
 */
@WebServlet("/api/counsellor/notifications")
public class CounsellorNotificationsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // GET - FETCH COUNSELLOR NOTIFICATIONS
    // =========================================================

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        Long counsellorId = getLoggedInCounsellorId(request);

        if (counsellorId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            out.print(
                "{\"success\":false,\"message\":\"Counsellor is not logged in.\"}"
            );
            return;
        }

        String filter = safe(request.getParameter("filter"));

        // Default = ALL
        // Supported:
        // ALL
        // UNREAD
        if (!filter.equalsIgnoreCase("UNREAD")) {
            filter = "ALL";
        }

        StringBuilder sql = new StringBuilder();

        sql.append("""
            SELECT
                notification_id,
                notification_type,
                title,
                message,
                reference_id,
                is_read,
                created_at
            FROM notifications
            WHERE user_id = ?
            """);

        if (filter.equalsIgnoreCase("UNREAD")) {
            sql.append(" AND is_read = 0 ");
        }

        sql.append("""
            ORDER BY created_at DESC, notification_id DESC
            """);

        String unreadSql = """
            SELECT COUNT(*)
            FROM notifications
            WHERE user_id = ?
              AND is_read = 0
            """;

        try (Connection con = DBConnection.getConnection()) {

            // -------------------------------------------------
            // GET UNREAD COUNT
            // -------------------------------------------------

            int unreadCount = 0;

            try (PreparedStatement psUnread =
                         con.prepareStatement(unreadSql)) {

                psUnread.setLong(1, counsellorId);

                try (ResultSet rs = psUnread.executeQuery()) {

                    if (rs.next()) {
                        unreadCount = rs.getInt(1);
                    }
                }
            }

            // -------------------------------------------------
            // GET NOTIFICATIONS
            // -------------------------------------------------

            StringBuilder json = new StringBuilder();

            json.append("{");
            json.append("\"success\":true,");
            json.append("\"filter\":\"")
                .append(jsonEscape(filter))
                .append("\",");
            json.append("\"unreadCount\":")
                .append(unreadCount)
                .append(",");

            json.append("\"count\":");

            int count = 0;

            // We'll first collect notification JSON objects.
            StringBuilder notificationsJson = new StringBuilder();
            notificationsJson.append("[");

            try (PreparedStatement ps =
                         con.prepareStatement(sql.toString())) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        if (count > 0) {
                            notificationsJson.append(",");
                        }

                        long notificationId =
                                rs.getLong("notification_id");

                        String notificationType =
                                safe(rs.getString("notification_type"));

                        String title =
                                safe(rs.getString("title"));

                        String message =
                                safe(rs.getString("message"));

                        long referenceId =
                                rs.getLong("reference_id");

                        boolean referenceIdWasNull =
                                rs.wasNull();

                        boolean isRead =
                                rs.getBoolean("is_read");

                        String createdAt =
                                safe(rs.getString("created_at"));

                        notificationsJson.append("{");

                        notificationsJson.append("\"notificationId\":")
                                .append(notificationId)
                                .append(",");

                        notificationsJson.append("\"type\":\"")
                                .append(jsonEscape(notificationType))
                                .append("\",");

                        notificationsJson.append("\"title\":\"")
                                .append(jsonEscape(title))
                                .append("\",");

                        notificationsJson.append("\"message\":\"")
                                .append(jsonEscape(message))
                                .append("\",");

                        if (referenceIdWasNull) {
                            notificationsJson.append(
                                    "\"referenceId\":null,"
                            );
                        } else {
                            notificationsJson.append(
                                    "\"referenceId\":"
                            ).append(referenceId).append(",");
                        }

                        notificationsJson.append("\"isRead\":")
                                .append(isRead)
                                .append(",");

                        notificationsJson.append("\"createdAt\":\"")
                                .append(jsonEscape(createdAt))
                                .append("\"");

                        notificationsJson.append("}");

                        count++;
                    }
                }
            }

            notificationsJson.append("]");

            json.append(count)
                .append(",");

            json.append("\"notifications\":")
                .append(notificationsJson);

            json.append("}");

            out.print(json);

        } catch (SQLException e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            out.print(
                "{\"success\":false,\"message\":\"Failed to load notifications.\"}"
            );
        }
    }

    // =========================================================
    // PUT - MARK NOTIFICATION READ / MARK ALL READ
    // =========================================================

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        Long counsellorId = getLoggedInCounsellorId(request);

        if (counsellorId == null) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            out.print(
                "{\"success\":false,\"message\":\"Counsellor is not logged in.\"}"
            );

            return;
        }

        // -----------------------------------------------------
        // MARK ALL AS READ
        // -----------------------------------------------------

        String markAllRead =
                safe(request.getParameter("markAllRead"));

        if (markAllRead.equalsIgnoreCase("true")) {

            String sql = """
                UPDATE notifications
                SET is_read = 1
                WHERE user_id = ?
                  AND is_read = 0
                """;

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setLong(1, counsellorId);

                int updated = ps.executeUpdate();

                out.print(
                    "{"
                    + "\"success\":true,"
                    + "\"message\":\"All notifications marked as read.\","
                    + "\"updated\":" + updated
                    + "}"
                );

            } catch (SQLException e) {

                e.printStackTrace();

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );

                out.print(
                    "{\"success\":false,\"message\":\"Failed to mark notifications as read.\"}"
                );
            }

            return;
        }

        // -----------------------------------------------------
        // MARK ONE NOTIFICATION AS READ
        // -----------------------------------------------------

        String notificationIdParam =
                safe(request.getParameter("notificationId"));

        if (notificationIdParam.isBlank()) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            out.print(
                "{\"success\":false,\"message\":\"notificationId is required.\"}"
            );

            return;
        }

        long notificationId;

        try {

            notificationId =
                    Long.parseLong(notificationIdParam);

        } catch (NumberFormatException e) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            out.print(
                "{\"success\":false,\"message\":\"Invalid notificationId.\"}"
            );

            return;
        }

        String sql = """
            UPDATE notifications
            SET is_read = 1
            WHERE notification_id = ?
              AND user_id = ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, notificationId);
            ps.setLong(2, counsellorId);

            int updated = ps.executeUpdate();

            if (updated == 0) {

                response.setStatus(
                        HttpServletResponse.SC_NOT_FOUND
                );

                out.print(
                    "{\"success\":false,\"message\":\"Notification not found.\"}"
                );

                return;
            }

            out.print(
                "{"
                + "\"success\":true,"
                + "\"message\":\"Notification marked as read.\""
                + "}"
            );

        } catch (SQLException e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            out.print(
                "{\"success\":false,\"message\":\"Failed to update notification.\"}"
            );
        }
    }

    // =========================================================
    // LOGIN SESSION CHECK
    // =========================================================

    private Long getLoggedInCounsellorId(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        Object roleObject =
                session.getAttribute("role");

        if (roleObject == null ||
            !"COUNSELLOR".equalsIgnoreCase(
                    String.valueOf(roleObject))) {

            return null;
        }

        Object userIdObject =
                session.getAttribute("userId");

        if (!(userIdObject instanceof Number)) {
            return null;
        }

        return ((Number) userIdObject).longValue();
    }

    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(String value) {

        return value == null ? "" : value.trim();
    }

    // =========================================================
    // JSON ESCAPE
    // =========================================================

    private String jsonEscape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
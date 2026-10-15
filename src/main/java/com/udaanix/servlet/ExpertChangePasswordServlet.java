package com.udaanix.servlet;

import com.udaanix.config.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/expert/change-password")
public class ExpertChangePasswordServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            HttpSession session = request.getSession(false);

            // =====================================================
            // 1. CHECK LOGIN SESSION
            // =====================================================

            if (session == null) {
                sendError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Please login first."
                );
                return;
            }

            String role = getSessionRole(session);

            if (!"EXPERT".equalsIgnoreCase(role)) {
                sendError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Only experts can change their password."
                );
                return;
            }

            Long userId = getSessionUserId(session);

            if (userId == null) {
                sendError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Expert session is invalid."
                );
                return;
            }

            // =====================================================
            // 2. GET FORM DATA
            // =====================================================

            String currentPassword =
                    request.getParameter("currentPassword");

            String newPassword =
                    request.getParameter("newPassword");

            String confirmPassword =
                    request.getParameter("confirmPassword");

            if (currentPassword == null ||
                newPassword == null ||
                confirmPassword == null ||
                currentPassword.isBlank() ||
                newPassword.isBlank() ||
                confirmPassword.isBlank()) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "All password fields are required."
                );
                return;
            }

            currentPassword = currentPassword.trim();
            newPassword = newPassword.trim();
            confirmPassword = confirmPassword.trim();

            // =====================================================
            // 3. PASSWORD LENGTH
            // =====================================================

            if (newPassword.length() < 8) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "New password must contain at least 8 characters."
                );
                return;
            }

            // =====================================================
            // 4. CONFIRM PASSWORD
            // =====================================================

            if (!newPassword.equals(confirmPassword)) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "New password and confirm password do not match."
                );
                return;
            }

            // =====================================================
            // 5. PREVENT SAME PASSWORD
            // =====================================================

            if (currentPassword.equals(newPassword)) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "New password must be different from the current password."
                );
                return;
            }

            // =====================================================
            // 6. DATABASE
            // =====================================================

            try (Connection connection =
                         DBConnection.getConnection()) {

                // -------------------------------------------------
                // Get current password hash
                // -------------------------------------------------

                String selectSQL = """
                    SELECT password_hash
                    FROM users
                    WHERE user_id = ?
                      AND role = 'EXPERT'
                      AND is_active = 1
                    """;

                String storedHash = null;

                try (PreparedStatement stmt =
                             connection.prepareStatement(selectSQL)) {

                    stmt.setLong(1, userId);

                    try (ResultSet rs =
                                 stmt.executeQuery()) {

                        if (!rs.next()) {

                            sendError(
                                    response,
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "Expert account was not found."
                            );
                            return;
                        }

                        storedHash =
                                rs.getString("password_hash");
                    }
                }

                if (storedHash == null ||
                    storedHash.isBlank()) {

                    sendError(
                            response,
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Password information is unavailable."
                    );
                    return;
                }

                // -------------------------------------------------
                // Verify current password
                // -------------------------------------------------

                boolean passwordMatches;

                try {
                    passwordMatches =
                            BCrypt.checkpw(
                                    currentPassword,
                                    storedHash
                            );
                } catch (IllegalArgumentException e) {
                    passwordMatches = false;
                }

                if (!passwordMatches) {

                    sendError(
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Current password is incorrect."
                    );
                    return;
                }

                // -------------------------------------------------
                // Generate new BCrypt hash
                // -------------------------------------------------

                String newPasswordHash =
                        BCrypt.hashpw(
                                newPassword,
                                BCrypt.gensalt(12)
                        );

                // -------------------------------------------------
                // Update password
                // -------------------------------------------------

                String updateSQL = """
                    UPDATE users
                    SET
                        password_hash = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE user_id = ?
                      AND role = 'EXPERT'
                      AND is_active = 1
                    """;

                int rowsUpdated;

                try (PreparedStatement stmt =
                             connection.prepareStatement(updateSQL)) {

                    stmt.setString(1, newPasswordHash);
                    stmt.setLong(2, userId);

                    rowsUpdated =
                            stmt.executeUpdate();
                }

                if (rowsUpdated == 0) {

                    sendError(
                            response,
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Password could not be updated."
                    );
                    return;
                }

                // =================================================
                // SUCCESS
                // =================================================

                response.setStatus(
                        HttpServletResponse.SC_OK
                );

                out.print("{");
                out.print("\"success\":true,");
                out.print("\"message\":");
                out.print(jsonString(
                        "Password changed successfully."
                ));
                out.print("}");

            } catch (Exception e) {

                e.printStackTrace();

                sendError(
                        response,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to change password. Please try again."
                );
            }
        }
    }

    // =========================================================
    // SESSION ROLE
    // =========================================================

    private String getSessionRole(HttpSession session) {

        Object role =
                session.getAttribute("role");

        return role != null
                ? role.toString()
                : null;
    }

    // =========================================================
    // SESSION USER ID
    // =========================================================

    private Long getSessionUserId(HttpSession session) {

        Object userId =
                session.getAttribute("userId");

        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }

        if (userId != null) {

            try {
                return Long.parseLong(
                        userId.toString()
                );
            } catch (NumberFormatException ignored) {
            }
        }

        // Fallback
        Object expertId =
                session.getAttribute("expertId");

        if (expertId instanceof Number) {
            return ((Number) expertId).longValue();
        }

        if (expertId != null) {

            try {
                return Long.parseLong(
                        expertId.toString()
                );
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    // =========================================================
    // JSON STRING
    // =========================================================

    private String jsonString(String value) {

        if (value == null) {
            return "null";
        }

        StringBuilder builder =
                new StringBuilder("\"");

        for (int i = 0; i < value.length(); i++) {

            char c = value.charAt(i);

            switch (c) {

                case '"':
                    builder.append("\\\"");
                    break;

                case '\\':
                    builder.append("\\\\");
                    break;

                case '\b':
                    builder.append("\\b");
                    break;

                case '\f':
                    builder.append("\\f");
                    break;

                case '\n':
                    builder.append("\\n");
                    break;

                case '\r':
                    builder.append("\\r");
                    break;

                case '\t':
                    builder.append("\\t");
                    break;

                default:

                    if (c < 32) {
                        builder.append(
                                String.format(
                                        "\\u%04x",
                                        (int) c
                                )
                        );
                    } else {
                        builder.append(c);
                    }
            }
        }

        builder.append("\"");

        return builder.toString();
    }

    // =========================================================
    // ERROR RESPONSE
    // =========================================================

    private void sendError(HttpServletResponse response,
                           int status,
                           String message)
            throws IOException {

        response.setStatus(status);

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        try (PrintWriter out =
                     response.getWriter()) {

            out.print("{");
            out.print("\"success\":false,");
            out.print("\"message\":");
            out.print(jsonString(message));
            out.print("}");
        }
    }
}
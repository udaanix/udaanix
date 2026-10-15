package com.udaanix.servlet;

import com.udaanix.config.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

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

@WebServlet("/api/auth/change-password")
public class AuthChangePasswordServlet extends HttpServlet {

    private static final int MIN_PASSWORD_LENGTH = 8;

    // =========================================================
    // GET
    // =========================================================

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

        try (PrintWriter out = response.getWriter()) {

            out.print("""
                {
                    "success": false,
                    "message": "Use POST to change your password."
                }
                """);
        }
    }

    // =========================================================
    // POST - CHANGE PASSWORD
    // =========================================================

    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        // =====================================================
        // 1. CHECK LOGIN SESSION
        // =====================================================

        HttpSession session = request.getSession(false);

        if (session == null ||
            session.getAttribute("userId") == null) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Your session has expired. Please log in again."
            );

            return;
        }

        // =====================================================
        // 2. GET USER ID FROM SESSION
        // =====================================================

        Object userIdObject =
                session.getAttribute("userId");

        if (!(userIdObject instanceof Number)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid login session."
            );

            return;
        }

        long userId =
                ((Number) userIdObject).longValue();

        // =====================================================
        // 3. GET PASSWORD VALUES
        // =====================================================

        String currentPassword =
                safe(request.getParameter("currentPassword"));

        String newPassword =
                safe(request.getParameter("newPassword"));

        String confirmPassword =
                safe(request.getParameter("confirmPassword"));

        // =====================================================
        // 4. BASIC VALIDATION
        // =====================================================

        if (currentPassword.isBlank()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Current password is required."
            );

            return;
        }

        if (newPassword.isBlank()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password is required."
            );

            return;
        }

        if (confirmPassword.isBlank()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Please confirm your new password."
            );

            return;
        }

        // =====================================================
        // 5. PASSWORD LENGTH
        // =====================================================

        if (newPassword.length() < MIN_PASSWORD_LENGTH) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password must contain at least 8 characters."
            );

            return;
        }

        // =====================================================
        // 6. PASSWORD STRENGTH
        // =====================================================

        if (!newPassword.matches(".*[A-Z].*")) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password must contain at least one uppercase letter."
            );

            return;
        }

        if (!newPassword.matches(".*[a-z].*")) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password must contain at least one lowercase letter."
            );

            return;
        }

        if (!newPassword.matches(".*[0-9].*")) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password must contain at least one number."
            );

            return;
        }

        // =====================================================
        // 7. CONFIRM PASSWORD
        // =====================================================

        if (!newPassword.equals(confirmPassword)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password and confirmation password do not match."
            );

            return;
        }

        // =====================================================
        // 8. NEW PASSWORD MUST BE DIFFERENT
        // =====================================================

        if (currentPassword.equals(newPassword)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password must be different from the current password."
            );

            return;
        }

        // =====================================================
        // 9. SQL QUERIES
        // =====================================================

        String selectSQL = """
            SELECT password_hash
            FROM users
            WHERE user_id = ?
              AND role = 'STUDENT'
              AND is_active = TRUE
            """;

        String updateSQL = """
            UPDATE users
            SET password_hash = ?
            WHERE user_id = ?
              AND role = 'STUDENT'
              AND is_active = TRUE
            """;

        // =====================================================
        // 10. DATABASE CONNECTION
        // =====================================================

        try (Connection connection =
                     DBConnection.getConnection()) {

            // =================================================
            // 11. GET EXISTING PASSWORD HASH
            // =================================================

            String storedHash;

            try (PreparedStatement ps =
                         connection.prepareStatement(selectSQL)) {

                ps.setLong(1, userId);

                try (ResultSet rs =
                             ps.executeQuery()) {

                    if (!rs.next()) {

                        sendError(
                                response,
                                out,
                                HttpServletResponse.SC_NOT_FOUND,
                                "Student account was not found."
                        );

                        return;
                    }

                    storedHash =
                            rs.getString("password_hash");
                }
            }

            // =================================================
            // 12. CHECK STORED PASSWORD HASH
            // =================================================

            if (storedHash == null ||
                storedHash.isBlank()) {

                sendError(
                        response,
                        out,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Password information is missing for this account."
                );

                return;
            }

            // =================================================
            // 13. VERIFY CURRENT PASSWORD USING BCrypt
            // =================================================

            boolean currentPasswordMatches;

            try {

                currentPasswordMatches =
                        BCrypt.checkpw(
                                currentPassword,
                                storedHash
                        );

            } catch (IllegalArgumentException ex) {

                currentPasswordMatches = false;
            }

            if (!currentPasswordMatches) {

                sendError(
                        response,
                        out,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Current password is incorrect."
                );

                return;
            }

            // =================================================
            // 14. HASH NEW PASSWORD
            // =================================================

            String newPasswordHash =
                    BCrypt.hashpw(
                            newPassword,
                            BCrypt.gensalt(12)
                    );

            // =================================================
            // 15. UPDATE PASSWORD IN DATABASE
            // =================================================

            try (PreparedStatement ps =
                         connection.prepareStatement(updateSQL)) {

                ps.setString(1, newPasswordHash);
                ps.setLong(2, userId);

                int updated =
                        ps.executeUpdate();

                if (updated != 1) {

                    sendError(
                            response,
                            out,
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Password could not be updated."
                    );

                    return;
                }
            }

            // =================================================
            // 16. SUCCESS RESPONSE
            // =================================================

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            out.print("""
                {
                    "success": true,
                    "message": "Password changed successfully."
                }
                """);

        } catch (Exception e) {

            e.printStackTrace();

            if (!response.isCommitted()) {

                sendError(
                        response,
                        out,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to change password. Please try again."
                );
            }
        }
    }

    // =========================================================
    // HELPER: SAFE STRING
    // =========================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value.trim();
    }

    // =========================================================
    // HELPER: ERROR RESPONSE
    // =========================================================

    private void sendError(HttpServletResponse response,
                           PrintWriter out,
                           int status,
                           String message) {

        response.setStatus(status);

        out.print(
                "{"
                + "\"success\":false,"
                + "\"message\":\""
                + escapeJson(message)
                + "\""
                + "}"
        );
    }

    // =========================================================
    // HELPER: JSON ESCAPE
    // =========================================================

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
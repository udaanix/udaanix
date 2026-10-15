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
import java.sql.SQLException;

/**
 * Counsellor Change Password Servlet
 *
 * Endpoint:
 * POST /api/counsellor/change-password
 *
 * Allows only the currently logged-in COUNSELLOR
 * to change their own password.
 */
@WebServlet("/api/counsellor/change-password")
public class CounsellorChangePasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

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

        // -----------------------------------------------------
        // CHECK SESSION
        // -----------------------------------------------------

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
        // GET FORM DATA
        // -----------------------------------------------------

        String currentPassword =
                safe(request.getParameter("currentPassword"));

        String newPassword =
                safe(request.getParameter("newPassword"));

        String confirmPassword =
                safe(request.getParameter("confirmPassword"));

        // -----------------------------------------------------
        // REQUIRED FIELD VALIDATION
        // -----------------------------------------------------

        if (currentPassword.isBlank() ||
            newPassword.isBlank() ||
            confirmPassword.isBlank()) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            out.print(
                "{\"success\":false,\"message\":\"All password fields are required.\"}"
            );

            return;
        }

        // -----------------------------------------------------
        // CONFIRM PASSWORD
        // -----------------------------------------------------

        if (!newPassword.equals(confirmPassword)) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            out.print(
                "{\"success\":false,\"message\":\"New password and confirm password do not match.\"}"
            );

            return;
        }

        // -----------------------------------------------------
        // PASSWORD LENGTH
        // -----------------------------------------------------

        if (newPassword.length() < 8) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            out.print(
                "{\"success\":false,\"message\":\"Password must contain at least 8 characters.\"}"
            );

            return;
        }

        // -----------------------------------------------------
        // PASSWORD STRENGTH
        // -----------------------------------------------------

        if (!containsUppercase(newPassword)) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            out.print(
                "{\"success\":false,\"message\":\"Password must contain at least one uppercase letter.\"}"
            );

            return;
        }

        if (!containsLowercase(newPassword)) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            out.print(
                "{\"success\":false,\"message\":\"Password must contain at least one lowercase letter.\"}"
            );

            return;
        }

        if (!containsNumber(newPassword)) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            out.print(
                "{\"success\":false,\"message\":\"Password must contain at least one number.\"}"
            );

            return;
        }

        // -----------------------------------------------------
        // PREVENT SAME PASSWORD
        // -----------------------------------------------------

        if (currentPassword.equals(newPassword)) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            out.print(
                "{\"success\":false,\"message\":\"New password must be different from your current password.\"}"
            );

            return;
        }

        // =====================================================
        // DATABASE
        // =====================================================

        String selectSql = """
            SELECT password_hash
            FROM users
            WHERE user_id = ?
              AND role = 'COUNSELLOR'
              AND is_active = TRUE
            """;

        String updateSql = """
            UPDATE users
            SET password_hash = ?
            WHERE user_id = ?
              AND role = 'COUNSELLOR'
              AND is_active = TRUE
            """;

        try (Connection con = DBConnection.getConnection()) {

            String existingHash;

            // -------------------------------------------------
            // GET CURRENT PASSWORD HASH
            // -------------------------------------------------

            try (PreparedStatement ps =
                         con.prepareStatement(selectSql)) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {

                        response.setStatus(
                                HttpServletResponse.SC_NOT_FOUND
                        );

                        out.print(
                            "{\"success\":false,\"message\":\"Counsellor account was not found.\"}"
                        );

                        return;
                    }

                    existingHash =
                            rs.getString("password_hash");
                }
            }

            // -------------------------------------------------
            // VERIFY CURRENT PASSWORD
            // -------------------------------------------------

            if (existingHash == null ||
                existingHash.isBlank() ||
                !BCrypt.checkpw(
                        currentPassword,
                        existingHash
                )) {

                response.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                out.print(
                    "{\"success\":false,\"message\":\"Current password is incorrect.\"}"
                );

                return;
            }

            // -------------------------------------------------
            // HASH NEW PASSWORD
            // -------------------------------------------------

            String newPasswordHash =
                    BCrypt.hashpw(
                            newPassword,
                            BCrypt.gensalt(12)
                    );

            // -------------------------------------------------
            // UPDATE PASSWORD
            // -------------------------------------------------

            try (PreparedStatement ps =
                         con.prepareStatement(updateSql)) {

                ps.setString(1, newPasswordHash);
                ps.setLong(2, counsellorId);

                int updated =
                        ps.executeUpdate();

                if (updated != 1) {

                    response.setStatus(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                    );

                    out.print(
                        "{\"success\":false,\"message\":\"Password could not be updated.\"}"
                    );

                    return;
                }
            }

            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

            out.print(
                "{"
                + "\"success\":true,"
                + "\"message\":\"Password changed successfully.\""
                + "}"
            );

        } catch (SQLException e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            out.print(
                "{\"success\":false,\"message\":\"Database error while changing password.\"}"
            );
        }
    }

    // =========================================================
    // GET LOGGED-IN COUNSELLOR ID
    // =========================================================

    private Long getLoggedInCounsellorId(
            HttpServletRequest request) {

        HttpSession session =
                request.getSession(false);

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
    // PASSWORD VALIDATION HELPERS
    // =========================================================

    private boolean containsUppercase(String value) {

        for (char c : value.toCharArray()) {

            if (Character.isUpperCase(c)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsLowercase(String value) {

        for (char c : value.toCharArray()) {

            if (Character.isLowerCase(c)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsNumber(String value) {

        for (char c : value.toCharArray()) {

            if (Character.isDigit(c)) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(String value) {

        return value == null ? "" : value.trim();
    }
}
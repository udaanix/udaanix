
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
import java.util.Locale;

@WebServlet("/api/auth/login")
public class AuthLoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String role = request.getParameter("role");

        try (PrintWriter out = response.getWriter()) {

            // Validate the submitted login details.
            if (email == null ||
                password == null ||
                role == null ||
                email.isBlank() ||
                password.isBlank() ||
                role.isBlank()) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST);

                out.println("""
                    {
                        "success": false,
                        "message": "Email, password and role are required."
                    }
                    """);

                return;
            }

            email = email.trim().toLowerCase(Locale.ROOT);
            role = role.trim().toUpperCase(Locale.ROOT);

            // Allow only the supported account roles.
            if (!role.equals("STUDENT") &&
                !role.equals("COUNSELLOR") &&
                !role.equals("EXPERT")) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST);

                out.println("""
                    {
                        "success": false,
                        "message": "Invalid account role."
                    }
                    """);

                return;
            }

            String sql = """
                SELECT
                    user_id,
                    role,
                    first_name,
                    last_name,
                    email,
                    mobile,
                    password_hash,
                    profile_picture,
                    is_active
                FROM users
                WHERE email = ?
                """;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, email);

                try (ResultSet rs = statement.executeQuery()) {

                    if (!rs.next()) {

                        response.setStatus(
                                HttpServletResponse.SC_UNAUTHORIZED);

                        out.println("""
                            {
                                "success": false,
                                "message": "Invalid email or password."
                            }
                            """);

                        return;
                    }

                    long userId = rs.getLong("user_id");

                    String dbRole = rs.getString("role");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String dbEmail = rs.getString("email");
                    String mobile = rs.getString("mobile");
                    String passwordHash = rs.getString("password_hash");
                    String profilePicture = rs.getString("profile_picture");
                    boolean isActive = rs.getBoolean("is_active");

                    // Reject inactive accounts.
                    if (!isActive) {

                        response.setStatus(
                                HttpServletResponse.SC_FORBIDDEN);

                        out.println("""
                            {
                                "success": false,
                                "message": "Your account is currently inactive."
                            }
                            """);

                        return;
                    }

                    // Verify the selected portal matches the account role.
                    if (dbRole == null ||
                        !dbRole.equalsIgnoreCase(role)) {

                        response.setStatus(
                                HttpServletResponse.SC_FORBIDDEN);

                        out.println("""
                            {
                                "success": false,
                                "message": "This account does not belong to the selected portal."
                            }
                            """);

                        return;
                    }

                    // Verify the BCrypt password hash.
                    boolean passwordMatches;

                    try {
                        passwordMatches =
                                passwordHash != null &&
                                !passwordHash.isBlank() &&
                                BCrypt.checkpw(password, passwordHash);

                    } catch (Exception e) {
                        passwordMatches = false;
                    }

                    if (!passwordMatches) {

                        response.setStatus(
                                HttpServletResponse.SC_UNAUTHORIZED);

                        out.println("""
                            {
                                "success": false,
                                "message": "Invalid email or password."
                            }
                            """);

                        return;
                    }

                    /*
                     * Authentication successful.
                     * Rotate an existing session ID to reduce
                     * session-fixation risk, or create a new session.
                     */
                    HttpSession session = request.getSession(false);

                    if (session != null) {
                        request.changeSessionId();
                    } else {
                        session = request.getSession(true);
                    }

                    // Existing session attributes: preserve these
                    // for current dashboards and APIs.
                    session.setAttribute("userId", userId);
                    session.setAttribute("role", dbRole);
                    session.setAttribute("firstName", firstName);
                    session.setAttribute("lastName", lastName);
                    session.setAttribute("email", dbEmail);
                    session.setAttribute("mobile", mobile);
                    session.setAttribute("profilePicture", profilePicture);

                    /*
                     * SIYA integration:
                     * These attributes provide a consistent identity
                     * contract for SIYA security utilities.
                     */
                    session.setAttribute("udaanixUserId", userId);
                    session.setAttribute(
                            "udaanixRole",
                            dbRole.toUpperCase(Locale.ROOT)
                    );

                    // Role-specific session IDs for existing APIs.
                    if ("STUDENT".equalsIgnoreCase(dbRole)) {

                        session.setAttribute("studentId", userId);

                    } else if ("COUNSELLOR".equalsIgnoreCase(dbRole)) {

                        session.setAttribute("counsellorId", userId);

                    } else if ("EXPERT".equalsIgnoreCase(dbRole)) {

                        session.setAttribute("expertId", userId);

                        String expertName =
                                ((firstName == null ? "" : firstName)
                                + " "
                                + (lastName == null ? "" : lastName))
                                .trim();

                        session.setAttribute("expertName", expertName);
                        session.setAttribute("expertEmail", dbEmail);
                        session.setAttribute(
                                "expertProfilePicture",
                                profilePicture
                        );
                    }

                    // Session expires after 30 minutes of inactivity.
                    session.setMaxInactiveInterval(30 * 60);

                    response.setStatus(HttpServletResponse.SC_OK);

                    /*
                     * Escape values before including them in JSON.
                     * No password or password hash is returned.
                     */
                    String safeFirstName = jsonEscape(firstName);
                    String safeLastName = jsonEscape(lastName);
                    String safeRole = jsonEscape(dbRole);

                    out.println("""
                        {
                            "success": true,
                            "message": "Login successful.",
                            "userId": %d,
                            "role": "%s",
                            "firstName": "%s",
                            "lastName": "%s"
                        }
                        """.formatted(
                            userId,
                            safeRole,
                            safeFirstName,
                            safeLastName
                        ));
                }
            }

        } catch (Exception e) {

            // Keep detailed errors in server logs, not in the response.
            getServletContext().log("Login request failed.", e);

            if (!response.isCommitted()) {
                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                response.setContentType("application/json;charset=UTF-8");

                response.getWriter().println("""
                    {
                        "success": false,
                        "message": "Login failed. Please try again."
                    }
                    """);
            }
        }
    }

    /**
     * Escapes strings for safe inclusion in a JSON string value.
     */
    private String jsonEscape(String value) {

        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {

            char ch = value.charAt(i);

            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(
                                String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }

        return escaped.toString();
    }
}

package com.udaanix.servlet;

import com.udaanix.config.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Expert Registration Servlet
 *
 * Endpoint:
 * POST /api/auth/expert-register
 *
 * Creates:
 * 1. users record
 * 2. experts record
 *
 * Password is stored using BCrypt.
 */
@WebServlet("/api/auth/expert-register")
public class ExpertRegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        try (PrintWriter out = response.getWriter()) {

            // =====================================================
            // 1. GET COMMON USER DETAILS
            // =====================================================

            String firstName =
                    clean(request.getParameter("firstName"));

            String lastName =
                    clean(request.getParameter("lastName"));

            String email =
                    clean(request.getParameter("email"))
                            .toLowerCase();

            String mobile =
                    clean(request.getParameter("mobile"));

            String password =
                    request.getParameter("password");

            String confirmPassword =
                    request.getParameter("confirmPassword");

            // =====================================================
            // 2. GET EXPERT DETAILS
            // =====================================================

            String jobTitle =
                    clean(request.getParameter("jobTitle"));

            String company =
                    clean(request.getParameter("company"));

            String industry =
                    clean(request.getParameter("industry"));

            String expertise =
                    clean(request.getParameter("expertise"));

            String stream =
                    clean(request.getParameter("stream"));

            String primaryCareer =
                    clean(request.getParameter("primaryCareer"));

            String city =
                    clean(request.getParameter("city"));

            String linkedinUrl =
                    clean(request.getParameter("linkedin"));

            String introVideoUrl =
                    clean(request.getParameter("introVideo"));

            String bio =
                    clean(request.getParameter("bio"));

            // =====================================================
            // 3. EXPERIENCE
            // =====================================================

            int experienceYears = 0;

            String experienceParam =
                    clean(request.getParameter("experience"));

            if (!experienceParam.isEmpty()) {

                try {

                    experienceYears =
                            parseExperienceYears(
                                    experienceParam
                            );

                } catch (IllegalArgumentException e) {

                    sendError(
                            response,
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Please select a valid professional experience."
                    );

                    return;
                }
            }

            if (experienceYears < 0) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Experience cannot be negative."
                );

                return;
            }

            // =====================================================
            // 4. BASIC VALIDATION
            // =====================================================

            if (firstName.isEmpty() ||
                lastName.isEmpty() ||
                email.isEmpty() ||
                password == null ||
                password.isBlank()) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "First name, last name, email and password are required."
                );

                return;
            }

            if (password.length() < 8) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Password must contain at least 8 characters."
                );

                return;
            }

            // =====================================================
            // 5. CONFIRM PASSWORD
            // =====================================================

            if (confirmPassword != null &&
                !password.equals(confirmPassword)) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Passwords do not match."
                );

                return;
            }

            // =====================================================
            // 6. EMAIL BASIC VALIDATION
            // =====================================================

            if (!email.matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Please enter a valid email address."
                );

                return;
            }

            // =====================================================
            // 7. DATABASE TRANSACTION
            // =====================================================

            try (Connection connection =
                         DBConnection.getConnection()) {

                connection.setAutoCommit(false);

                try {

                    // -------------------------------------------------
                    // Check duplicate email
                    // -------------------------------------------------

                    String checkEmailSQL = """
                        SELECT user_id
                        FROM users
                        WHERE email = ?
                        LIMIT 1
                        """;

                    try (PreparedStatement stmt =
                                 connection.prepareStatement(
                                         checkEmailSQL)) {

                        stmt.setString(1, email);

                        try (ResultSet rs =
                                     stmt.executeQuery()) {

                            if (rs.next()) {

                                connection.rollback();

                                response.setStatus(
                                        HttpServletResponse.SC_CONFLICT
                                );

                                out.print("{");
                                out.print("\"success\":false,");
                                out.print("\"message\":");
                                out.print(jsonString(
                                        "An account with this email already exists."
                                ));
                                out.print("}");

                                return;
                            }
                        }
                    }

                    // -------------------------------------------------
                    // BCrypt password
                    // -------------------------------------------------

                    String passwordHash =
                            BCrypt.hashpw(
                                    password,
                                    BCrypt.gensalt(12)
                            );

                    // -------------------------------------------------
                    // Insert into users
                    // -------------------------------------------------

                    String userSQL = """
                        INSERT INTO users
                        (
                            role,
                            first_name,
                            last_name,
                            email,
                            mobile,
                            password_hash
                        )
                        VALUES
                        (
                            'EXPERT',
                            ?,
                            ?,
                            ?,
                            ?,
                            ?
                        )
                        """;

                    long userId;

                    try (PreparedStatement stmt =
                                 connection.prepareStatement(
                                         userSQL,
                                         Statement.RETURN_GENERATED_KEYS
                                 )) {

                        stmt.setString(1, firstName);
                        stmt.setString(2, lastName);
                        stmt.setString(3, email);

                        if (mobile.isEmpty()) {
                            stmt.setNull(
                                    4,
                                    java.sql.Types.VARCHAR
                            );
                        } else {
                            stmt.setString(4, mobile);
                        }

                        stmt.setString(
                                5,
                                passwordHash
                        );

                        stmt.executeUpdate();

                        try (ResultSet keys =
                                     stmt.getGeneratedKeys()) {

                            if (!keys.next()) {

                                throw new Exception(
                                        "Unable to create user account."
                                );
                            }

                            userId =
                                    keys.getLong(1);
                        }
                    }

                    // -------------------------------------------------
                    // Insert into experts
                    // -------------------------------------------------

                    String expertSQL = """
                        INSERT INTO experts
                        (
                            expert_id,
                            job_title,
                            company,
                            industry,
                            expertise,
                            stream,
                            primary_career,
                            experience_years,
                            city,
                            linkedin_url,
                            intro_video_url,
                            bio,
                            session_fee,
                            session_duration_minutes,
                            rating,
                            review_count,
                            students_mentored,
                            is_verified,
                            availability_status
                        )
                        VALUES
                        (
                            ?,
                            ?,
                            ?,
                            ?,
                            ?,
                            ?,
                            ?,
                            ?,
                            ?,
                            ?,
                            ?,
                            ?,
                            0.00,
                            30,
                            0.00,
                            0,
                            0,
                            0,
                            'Available'
                        )
                        """;

                    try (PreparedStatement stmt =
                                 connection.prepareStatement(
                                         expertSQL)) {

                        stmt.setLong(1, userId);

                        setNullableString(
                                stmt,
                                2,
                                jobTitle
                        );

                        setNullableString(
                                stmt,
                                3,
                                company
                        );

                        setNullableString(
                                stmt,
                                4,
                                industry
                        );

                        setNullableString(
                                stmt,
                                5,
                                expertise
                        );

                        setNullableString(
                                stmt,
                                6,
                                stream
                        );

                        setNullableString(
                                stmt,
                                7,
                                primaryCareer
                        );

                        stmt.setInt(
                                8,
                                experienceYears
                        );

                        setNullableString(
                                stmt,
                                9,
                                city
                        );

                        setNullableString(
                                stmt,
                                10,
                                linkedinUrl
                        );

                        setNullableString(
                                stmt,
                                11,
                                introVideoUrl
                        );

                        setNullableString(
                                stmt,
                                12,
                                bio
                        );

                        stmt.executeUpdate();
                    }

                    // -------------------------------------------------
                    // Commit
                    // -------------------------------------------------

                    connection.commit();

                    // =================================================
                    // SUCCESS RESPONSE
                    // =================================================

                    response.setStatus(
                            HttpServletResponse.SC_CREATED
                    );

                    out.print("{");
                    out.print("\"success\":true,");
                    out.print("\"message\":");
                    out.print(jsonString(
                            "Industry Expert account created successfully."
                    ));
                    out.print(",");
                    out.print("\"userId\":"
                            + userId);
                    out.print("}");

                } catch (Exception e) {

                    connection.rollback();

                    throw e;
                }

            } catch (Exception e) {

                e.printStackTrace();

                sendError(
                        response,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Expert registration failed. Please try again."
                );
            }
        }
    }

    // =========================================================
    // CLEAN INPUT
    // =========================================================

    private String clean(String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    // =========================================================
    // EXPERIENCE RANGE PARSER
    // =========================================================

    private int parseExperienceYears(String value) {

        String normalized =
                value.trim()
                        .replace("\u2013", "-")   // en dash
                        .replace("\u2014", "-")   // em dash
                        .replace("\u2212", "-");  // minus sign

        // Frontend sends values such as:
        // 0-2 Years
        // 3-5 Years
        // 6-10 Years
        // 11-15 Years
        // 15+ Years

        if (normalized.matches("0\\s*-\\s*2\\s*Years?")) {
            return 2;
        }

        if (normalized.matches("3\\s*-\\s*5\\s*Years?")) {
            return 5;
        }

        if (normalized.matches("6\\s*-\\s*10\\s*Years?")) {
            return 10;
        }

        if (normalized.matches("11\\s*-\\s*15\\s*Years?")) {
            return 15;
        }

        if (normalized.matches("15\\s*\\+\\s*Years?")) {
            return 15;
        }

        // Also allow a plain numeric value if another frontend
        // sends one in the future, e.g. "8".
        if (normalized.matches("\\d+")) {
            return Integer.parseInt(normalized);
        }

        throw new IllegalArgumentException(
                "Invalid experience value: " + value
        );
    }

    // =========================================================
    // NULLABLE STRING
    // =========================================================

    private void setNullableString(
            PreparedStatement stmt,
            int index,
            String value)
            throws java.sql.SQLException {

        if (value == null ||
            value.isBlank()) {

            stmt.setNull(
                    index,
                    java.sql.Types.VARCHAR
            );

        } else {

            stmt.setString(
                    index,
                    value
            );
        }
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

        for (int i = 0;
             i < value.length();
             i++) {

            char c =
                    value.charAt(i);

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

    private void sendError(
            HttpServletResponse response,
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
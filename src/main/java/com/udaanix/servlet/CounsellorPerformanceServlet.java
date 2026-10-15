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
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Counsellor Performance Servlet
 *
 * Endpoint:
 * GET /api/counsellor/performance
 *
 * Provides real performance statistics for the logged-in counsellor.
 */
@WebServlet("/api/counsellor/performance")
public class CounsellorPerformanceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // GET PERFORMANCE
    // =========================================================

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        Long counsellorId = getLoggedInCounsellorId(request);

        // -----------------------------------------------------
        // LOGIN CHECK
        // -----------------------------------------------------

        if (counsellorId == null) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            out.print(
                "{\"success\":false,\"message\":\"Counsellor is not logged in.\"}"
            );

            return;
        }

        // =====================================================
        // DEFAULT VALUES
        // =====================================================

        int totalStudents = 0;
        int totalSessions = 0;
        int completedSessions = 0;
        int upcomingSessions = 0;

        int totalReviews = 0;
        double averageRating = 0.0;

        int profileCompletion = 0;

        try (Connection con = DBConnection.getConnection()) {

            // =================================================
            // 1. STUDENTS CONNECTED
            // =================================================
            //
            // A student is considered connected if the
            // counsellor has a counselling session with them.
            //
            // DISTINCT prevents the same student being counted
            // multiple times for multiple sessions.
            // =================================================

            String studentsSql = """
                SELECT COUNT(DISTINCT student_id)
                FROM counselling_sessions
                WHERE counsellor_id = ?
                """;

            try (PreparedStatement ps =
                         con.prepareStatement(studentsSql)) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        totalStudents = rs.getInt(1);
                    }
                }
            }

            // =================================================
            // 2. TOTAL SESSIONS
            // =================================================

            String totalSessionsSql = """
                SELECT COUNT(*)
                FROM counselling_sessions
                WHERE counsellor_id = ?
                """;

            try (PreparedStatement ps =
                         con.prepareStatement(totalSessionsSql)) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        totalSessions = rs.getInt(1);
                    }
                }
            }

            // =================================================
            // 3. COMPLETED SESSIONS
            // =================================================

            String completedSql = """
                SELECT COUNT(*)
                FROM counselling_sessions
                WHERE counsellor_id = ?
                  AND status = 'COMPLETED'
                """;

            try (PreparedStatement ps =
                         con.prepareStatement(completedSql)) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        completedSessions = rs.getInt(1);
                    }
                }
            }

            // =================================================
            // 4. UPCOMING SESSIONS
            // =================================================
            //
            // We use scheduled_at > current time and exclude
            // cancelled/completed sessions.
            // =================================================

            String upcomingSql = """
                SELECT COUNT(*)
                FROM counselling_sessions
                WHERE counsellor_id = ?
                  AND scheduled_at > NOW()
                  AND status NOT IN ('CANCELLED', 'COMPLETED')
                """;

            try (PreparedStatement ps =
                         con.prepareStatement(upcomingSql)) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        upcomingSessions = rs.getInt(1);
                    }
                }
            }

            // =================================================
            // 5. REVIEWS + AVERAGE RATING
            // =================================================

            String reviewsSql = """
                SELECT
                    COUNT(*) AS total_reviews,
                    COALESCE(AVG(rating), 0) AS average_rating
                FROM counsellor_reviews
                WHERE counsellor_id = ?
                  AND is_visible = 1
                """;

            try (PreparedStatement ps =
                         con.prepareStatement(reviewsSql)) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        totalReviews =
                                rs.getInt("total_reviews");

                        averageRating =
                                rs.getDouble("average_rating");
                    }
                }
            }

            // =================================================
            // 6. PROFILE COMPLETION
            // =================================================
            //
            // Based on actual counsellor profile fields.
            //
            // Checks:
            // First Name
            // Last Name
            // Email
            // Mobile
            // Qualification
            // Specialization
            // Experience
            // City
            // Bio
            // Profile Picture
            // =================================================

            String profileSql = """
                SELECT
                    u.first_name,
                    u.last_name,
                    u.email,
                    u.mobile,
                    u.profile_picture,
                    c.qualification,
                    c.specialization,
                    c.experience_years,
                    c.city,
                    c.bio
                FROM users u
                INNER JOIN counsellors c
                    ON c.counsellor_id = u.user_id
                WHERE u.user_id = ?
                  AND u.role = 'COUNSELLOR'
                """;

            try (PreparedStatement ps =
                         con.prepareStatement(profileSql)) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        int completedFields = 0;
                        int totalFields = 10;

                        // First name
                        if (!safe(rs.getString("first_name"))
                                .isBlank()) {
                            completedFields++;
                        }

                        // Last name
                        if (!safe(rs.getString("last_name"))
                                .isBlank()) {
                            completedFields++;
                        }

                        // Email
                        if (!safe(rs.getString("email"))
                                .isBlank()) {
                            completedFields++;
                        }

                        // Mobile
                        if (!safe(rs.getString("mobile"))
                                .isBlank()) {
                            completedFields++;
                        }

                        // Qualification
                        if (!safe(rs.getString("qualification"))
                                .isBlank()) {
                            completedFields++;
                        }

                        // Specialization
                        if (!safe(rs.getString("specialization"))
                                .isBlank()) {
                            completedFields++;
                        }

                        // Experience
                        int experience =
                                rs.getInt("experience_years");

                        if (!rs.wasNull() && experience >= 0) {
                            completedFields++;
                        }

                        // City
                        if (!safe(rs.getString("city"))
                                .isBlank()) {
                            completedFields++;
                        }

                        // Bio
                        if (!safe(rs.getString("bio"))
                                .isBlank()) {
                            completedFields++;
                        }

                        // Profile picture
                        if (!safe(rs.getString("profile_picture"))
                                .isBlank()) {
                            completedFields++;
                        }

                        profileCompletion =
                                (int) Math.round(
                                    (completedFields * 100.0)
                                    / totalFields
                                );
                    }
                }
            }

            // =================================================
            // 7. SESSION COMPLETION RATE
            // =================================================

            double sessionCompletionRate = 0.0;

            if (totalSessions > 0) {

                sessionCompletionRate =
                        (completedSessions * 100.0)
                        / totalSessions;
            }

            // =================================================
            // 8. RATING SCORE
            // =================================================

            double ratingScore = 0.0;

            if (totalReviews > 0) {

                ratingScore =
                        (averageRating / 5.0) * 100.0;
            }

            // =================================================
            // 9. OVERALL PERFORMANCE
            // =================================================
            //
            // If there are no reviews/sessions yet, we don't
            // manufacture a score.
            //
            // Performance is based on:
            // - Session completion: 50%
            // - Rating: 50%
            //
            // Profile completion is shown separately.
            // =================================================

            double overallPerformance = 0.0;

            boolean hasPerformanceData =
                    totalSessions > 0 ||
                    totalReviews > 0;

            if (hasPerformanceData) {

                overallPerformance =
                        (sessionCompletionRate * 0.50)
                        + (ratingScore * 0.50);
            }

            // Keep values within 0-100
            overallPerformance =
                    Math.max(
                        0,
                        Math.min(
                            100,
                            overallPerformance
                        )
                    );

            // =================================================
            // 10. FINAL JSON
            // =================================================

            StringBuilder json =
                    new StringBuilder();

            json.append("{");

            json.append("\"success\":true,");

            json.append("\"counsellorId\":")
                    .append(counsellorId)
                    .append(",");

            json.append("\"students\":{");

            json.append("\"total\":")
                    .append(totalStudents);

            json.append("},");

            json.append("\"sessions\":{");

            json.append("\"total\":")
                    .append(totalSessions)
                    .append(",");

            json.append("\"completed\":")
                    .append(completedSessions)
                    .append(",");

            json.append("\"upcoming\":")
                    .append(upcomingSessions)
                    .append(",");

            json.append("\"completionRate\":")
                    .append(formatDouble(
                            sessionCompletionRate
                    ));

            json.append("},");

            json.append("\"reviews\":{");

            json.append("\"total\":")
                    .append(totalReviews)
                    .append(",");

            json.append("\"averageRating\":")
                    .append(formatDouble(
                            averageRating
                    ));

            json.append("},");

            json.append("\"profileCompletion\":")
                    .append(profileCompletion)
                    .append(",");

            json.append("\"overallPerformance\":")
                    .append(formatDouble(
                            overallPerformance
                    ));

            json.append("}");

            out.print(json);

        } catch (SQLException e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            out.print(
                "{\"success\":false,\"message\":\"Failed to load counsellor performance.\"}"
            );
        }
    }

    // =========================================================
    // LOGIN SESSION CHECK
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
    // SAFE STRING
    // =========================================================

    private String safe(String value) {

        return value == null ? "" : value.trim();
    }

    // =========================================================
    // FORMAT NUMBER
    // =========================================================

    private String formatDouble(double value) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }
}
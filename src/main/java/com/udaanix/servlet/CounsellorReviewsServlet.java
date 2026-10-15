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
 * Counsellor Reviews Servlet
 *
 * GET:
 * /api/counsellor/reviews
 *
 * Returns:
 * - Total reviews
 * - Average rating
 * - Rating distribution
 * - Individual visible reviews
 *
 * Only reviews belonging to the logged-in counsellor
 * are returned.
 */
@WebServlet("/api/counsellor/reviews")
public class CounsellorReviewsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // GET REVIEWS
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
        // SQL
        // =====================================================

        String reviewsSql = """
            SELECT
                cr.review_id,
                cr.student_id,
                cr.session_id,
                cr.rating,
                cr.review_text,
                cr.created_at,
                u.first_name,
                u.last_name,
                u.profile_picture
            FROM counsellor_reviews cr
            LEFT JOIN users u
                ON u.user_id = cr.student_id
            WHERE cr.counsellor_id = ?
              AND cr.is_visible = 1
            ORDER BY cr.created_at DESC, cr.review_id DESC
            """;

        String summarySql = """
            SELECT
                COUNT(*) AS total_reviews,
                COALESCE(AVG(rating), 0) AS average_rating,
                SUM(CASE WHEN rating = 5 THEN 1 ELSE 0 END) AS five_star,
                SUM(CASE WHEN rating = 4 THEN 1 ELSE 0 END) AS four_star,
                SUM(CASE WHEN rating = 3 THEN 1 ELSE 0 END) AS three_star,
                SUM(CASE WHEN rating = 2 THEN 1 ELSE 0 END) AS two_star,
                SUM(CASE WHEN rating = 1 THEN 1 ELSE 0 END) AS one_star
            FROM counsellor_reviews
            WHERE counsellor_id = ?
              AND is_visible = 1
            """;

        try (Connection con = DBConnection.getConnection()) {

            // =================================================
            // SUMMARY
            // =================================================

            int totalReviews = 0;
            double averageRating = 0.0;

            int fiveStar = 0;
            int fourStar = 0;
            int threeStar = 0;
            int twoStar = 0;
            int oneStar = 0;

            try (PreparedStatement ps =
                         con.prepareStatement(summarySql)) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        totalReviews =
                                rs.getInt("total_reviews");

                        averageRating =
                                rs.getDouble("average_rating");

                        fiveStar =
                                rs.getInt("five_star");

                        fourStar =
                                rs.getInt("four_star");

                        threeStar =
                                rs.getInt("three_star");

                        twoStar =
                                rs.getInt("two_star");

                        oneStar =
                                rs.getInt("one_star");
                    }
                }
            }

            // =================================================
            // REVIEWS
            // =================================================

            StringBuilder reviewsJson =
                    new StringBuilder();

            reviewsJson.append("[");

            int reviewCount = 0;

            try (PreparedStatement ps =
                         con.prepareStatement(reviewsSql)) {

                ps.setLong(1, counsellorId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        if (reviewCount > 0) {
                            reviewsJson.append(",");
                        }

                        long reviewId =
                                rs.getLong("review_id");

                        long studentId =
                                rs.getLong("student_id");

                        int rating =
                                rs.getInt("rating");

                        String reviewText =
                                safe(rs.getString("review_text"));

                        String firstName =
                                safe(rs.getString("first_name"));

                        String lastName =
                                safe(rs.getString("last_name"));

                        String profilePicture =
                                safe(rs.getString("profile_picture"));

                        String createdAt =
                                safe(rs.getString("created_at"));

                        String studentName =
                                (firstName + " " + lastName).trim();

                        if (studentName.isBlank()) {
                            studentName = "Student";
                        }

                        reviewsJson.append("{");

                        reviewsJson.append(
                                "\"reviewId\":"
                        ).append(reviewId).append(",");

                        reviewsJson.append(
                                "\"studentId\":"
                        ).append(studentId).append(",");

                        reviewsJson.append(
                                "\"studentName\":\""
                        ).append(
                                jsonEscape(studentName)
                        ).append("\",");

                        reviewsJson.append(
                                "\"rating\":"
                        ).append(rating).append(",");

                        reviewsJson.append(
                                "\"reviewText\":\""
                        ).append(
                                jsonEscape(reviewText)
                        ).append("\",");

                        reviewsJson.append(
                                "\"profilePicture\":\""
                        ).append(
                                jsonEscape(profilePicture)
                        ).append("\",");

                        reviewsJson.append(
                                "\"sessionId\":"
                        );

                        Object sessionId =
                                rs.getObject("session_id");

                        if (sessionId == null) {
                            reviewsJson.append("null,");
                        } else {
                            reviewsJson.append(
                                    sessionId
                            ).append(",");
                        }

                        reviewsJson.append(
                                "\"createdAt\":\""
                        ).append(
                                jsonEscape(createdAt)
                        ).append("\"");

                        reviewsJson.append("}");

                        reviewCount++;
                    }
                }
            }

            reviewsJson.append("]");

            // =================================================
            // FINAL JSON
            // =================================================

            StringBuilder json =
                    new StringBuilder();

            json.append("{");

            json.append("\"success\":true,");

            json.append(
                    "\"totalReviews\":"
            ).append(totalReviews).append(",");

            json.append(
                    "\"averageRating\":"
            ).append(
                    formatDouble(averageRating)
            ).append(",");

            json.append("\"ratingDistribution\":{");

            json.append("\"5\":")
                    .append(fiveStar)
                    .append(",");

            json.append("\"4\":")
                    .append(fourStar)
                    .append(",");

            json.append("\"3\":")
                    .append(threeStar)
                    .append(",");

            json.append("\"2\":")
                    .append(twoStar)
                    .append(",");

            json.append("\"1\":")
                    .append(oneStar);

            json.append("},");

            json.append(
                    "\"count\":"
            ).append(reviewCount).append(",");

            json.append(
                    "\"reviews\":"
            ).append(reviewsJson);

            json.append("}");

            out.print(json);

        } catch (SQLException e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            out.print(
                "{\"success\":false,\"message\":\"Failed to load counsellor reviews.\"}"
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

    // =========================================================
    // FORMAT DOUBLE
    // =========================================================

    private String formatDouble(double value) {

        return String.format(
                java.util.Locale.US,
                "%.2f",
                value
        );
    }
}
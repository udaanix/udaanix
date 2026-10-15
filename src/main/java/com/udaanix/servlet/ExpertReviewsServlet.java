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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Expert Reviews Servlet
 *
 * GET:
 *     /api/expert/reviews
 *
 *     Returns all reviews received by the logged-in expert.
 *
 * POST:
 *     /api/expert/reviews
 *
 *     Allows a logged-in student to submit a review
 *     for a completed expert booking.
 */
@WebServlet("/api/expert/reviews")
public class ExpertReviewsServlet extends HttpServlet {

    // =========================================================
    // GET - FETCH EXPERT REVIEWS
    // =========================================================

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);

        try (PrintWriter out = response.getWriter()) {

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
                        "Only experts can view expert reviews."
                );
                return;
            }

            Long expertId = getSessionUserId(session);

            if (expertId == null) {
                sendError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Expert session is invalid."
                );
                return;
            }

            try (Connection connection = DBConnection.getConnection()) {

                // -------------------------------------------------
                // Expert summary
                // -------------------------------------------------

                String summarySQL = """
                    SELECT
                        rating,
                        review_count
                    FROM experts
                    WHERE expert_id = ?
                    """;

                BigDecimal averageRating = BigDecimal.ZERO;
                int reviewCount = 0;

                try (PreparedStatement stmt =
                             connection.prepareStatement(summarySQL)) {

                    stmt.setLong(1, expertId);

                    try (ResultSet rs = stmt.executeQuery()) {

                        if (rs.next()) {

                            BigDecimal dbRating =
                                    rs.getBigDecimal("rating");

                            if (dbRating != null) {
                                averageRating = dbRating;
                            }

                            reviewCount =
                                    rs.getInt("review_count");
                        }
                    }
                }

                // -------------------------------------------------
                // Reviews
                // -------------------------------------------------

                String reviewSQL = """
                    SELECT
                        er.review_id,
                        er.booking_id,
                        er.student_id,
                        er.expert_id,
                        er.rating,
                        er.review_text,
                        er.created_at,
                        er.updated_at,
                        u.first_name,
                        u.last_name,
                        u.email,
                        u.profile_picture
                    FROM expert_reviews er
                    INNER JOIN users u
                        ON u.user_id = er.student_id
                    WHERE er.expert_id = ?
                    ORDER BY er.created_at DESC
                    """;

                try (PreparedStatement stmt =
                             connection.prepareStatement(reviewSQL)) {

                    stmt.setLong(1, expertId);

                    try (ResultSet rs = stmt.executeQuery()) {

                        out.print("{");
                        out.print("\"success\":true,");
                        out.print("\"averageRating\":"
                                + averageRating.toPlainString()
                                + ",");
                        out.print("\"reviewCount\":"
                                + reviewCount
                                + ",");
                        out.print("\"reviews\":[");

                        boolean first = true;

                        while (rs.next()) {

                            if (!first) {
                                out.print(",");
                            }

                            first = false;

                            out.print("{");

                            out.print("\"reviewId\":"
                                    + rs.getInt("review_id")
                                    + ",");

                            out.print("\"bookingId\":"
                                    + rs.getInt("booking_id")
                                    + ",");

                            out.print("\"studentId\":"
                                    + rs.getInt("student_id")
                                    + ",");

                            out.print("\"expertId\":"
                                    + rs.getInt("expert_id")
                                    + ",");

                            BigDecimal rating =
                                    rs.getBigDecimal("rating");

                            out.print("\"rating\":"
                                    + (rating == null
                                    ? "0"
                                    : rating.toPlainString())
                                    + ",");

                            out.print("\"reviewText\":");
                            out.print(jsonString(
                                    rs.getString("review_text")
                            ));
                            out.print(",");

                            out.print("\"studentName\":");
                            out.print(jsonString(
                                    buildFullName(
                                            rs.getString("first_name"),
                                            rs.getString("last_name")
                                    )
                            ));
                            out.print(",");

                            out.print("\"studentEmail\":");
                            out.print(jsonString(
                                    rs.getString("email")
                            ));
                            out.print(",");

                            out.print("\"profilePicture\":");
                            out.print(jsonString(
                                    rs.getString("profile_picture")
                            ));
                            out.print(",");

                            out.print("\"createdAt\":");
                            out.print(jsonString(
                                    rs.getTimestamp("created_at")
                                            != null
                                            ? rs.getTimestamp(
                                                    "created_at"
                                              ).toString()
                                            : null
                            ));
                            out.print(",");

                            out.print("\"updatedAt\":");
                            out.print(jsonString(
                                    rs.getTimestamp("updated_at")
                                            != null
                                            ? rs.getTimestamp(
                                                    "updated_at"
                                              ).toString()
                                            : null
                            ));

                            out.print("}");
                        }

                        out.print("]");
                        out.print("}");
                    }
                }

            } catch (Exception e) {

                e.printStackTrace();

                sendError(
                        response,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Unable to load expert reviews."
                );
            }
        }
    }

    // =========================================================
    // POST - STUDENT SUBMITS REVIEW
    // =========================================================

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);

        try (PrintWriter out = response.getWriter()) {

            if (session == null) {
                sendError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Please login first."
                );
                return;
            }

            String role = getSessionRole(session);

            if (!"STUDENT".equalsIgnoreCase(role)) {
                sendError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Only students can submit expert reviews."
                );
                return;
            }

            Long studentId = getSessionUserId(session);

            if (studentId == null) {
                sendError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Student session is invalid."
                );
                return;
            }

            // -------------------------------------------------
            // Request parameters
            // -------------------------------------------------

            String bookingIdParam =
                    request.getParameter("bookingId");

            String ratingParam =
                    request.getParameter("rating");

            String reviewText =
                    request.getParameter("reviewText");

            if (bookingIdParam == null ||
                ratingParam == null ||
                bookingIdParam.isBlank() ||
                ratingParam.isBlank()) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Booking ID and rating are required."
                );
                return;
            }

            long bookingId;

            try {
                bookingId = Long.parseLong(
                        bookingIdParam.trim()
                );
            } catch (NumberFormatException e) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid booking ID."
                );
                return;
            }

            BigDecimal rating;

            try {
                rating = new BigDecimal(
                        ratingParam.trim()
                );
            } catch (NumberFormatException e) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid rating."
                );
                return;
            }

            // -------------------------------------------------
            // Rating validation
            // -------------------------------------------------

            if (rating.compareTo(BigDecimal.ONE) < 0 ||
                rating.compareTo(new BigDecimal("5.00")) > 0) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Rating must be between 1 and 5."
                );
                return;
            }

            // Only allow maximum 2 decimal places
            if (rating.scale() > 2) {

                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Rating can have maximum 2 decimal places."
                );
                return;
            }

            if (reviewText != null) {
                reviewText = reviewText.trim();

                if (reviewText.length() > 5000) {

                    sendError(
                            response,
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Review text is too long."
                    );
                    return;
                }
            }

            // -------------------------------------------------
            // Database transaction
            // -------------------------------------------------

            try (Connection connection =
                         DBConnection.getConnection()) {

                connection.setAutoCommit(false);

                try {

                    // -------------------------------------------------
                    // 1. Verify booking
                    // -------------------------------------------------

                    String bookingSQL = """
                        SELECT
                            booking_id,
                            student_id,
                            expert_id,
                            status
                        FROM expert_bookings
                        WHERE booking_id = ?
                        FOR UPDATE
                        """;

                    long expertId;

                    try (PreparedStatement stmt =
                                 connection.prepareStatement(
                                         bookingSQL)) {

                        stmt.setLong(1, bookingId);

                        try (ResultSet rs =
                                     stmt.executeQuery()) {

                            if (!rs.next()) {

                                connection.rollback();

                                sendError(
                                        response,
                                        HttpServletResponse.SC_NOT_FOUND,
                                        "Booking not found."
                                );
                                return;
                            }

                            long bookingStudentId =
                                    rs.getLong("student_id");

                            expertId =
                                    rs.getLong("expert_id");

                            String status =
                                    rs.getString("status");

                            // Booking must belong to logged-in student
                            if (bookingStudentId != studentId) {

                                connection.rollback();

                                sendError(
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "You cannot review this booking."
                                );
                                return;
                            }

                            // Review allowed only after completed session
                            if (!"COMPLETED".equalsIgnoreCase(status)) {

                                connection.rollback();

                                sendError(
                                        response,
                                        HttpServletResponse.SC_BAD_REQUEST,
                                        "You can review an expert only after the session is completed."
                                );
                                return;
                            }
                        }
                    }

                    // -------------------------------------------------
                    // 2. Check duplicate review
                    // -------------------------------------------------

                    String duplicateSQL = """
                        SELECT review_id
                        FROM expert_reviews
                        WHERE booking_id = ?
                        LIMIT 1
                        """;

                    try (PreparedStatement stmt =
                                 connection.prepareStatement(
                                         duplicateSQL)) {

                        stmt.setLong(1, bookingId);

                        try (ResultSet rs =
                                     stmt.executeQuery()) {

                            if (rs.next()) {

                                connection.rollback();

                                sendError(
                                        response,
                                        HttpServletResponse.SC_CONFLICT,
                                        "You have already reviewed this session."
                                );
                                return;
                            }
                        }
                    }

                    // -------------------------------------------------
                    // 3. Insert review
                    // -------------------------------------------------

                    String insertSQL = """
                        INSERT INTO expert_reviews
                        (
                            booking_id,
                            student_id,
                            expert_id,
                            rating,
                            review_text
                        )
                        VALUES (?, ?, ?, ?, ?)
                        """;

                    try (PreparedStatement stmt =
                                 connection.prepareStatement(
                                         insertSQL)) {

                        stmt.setLong(1, bookingId);
                        stmt.setLong(2, studentId);
                        stmt.setLong(3, expertId);
                        stmt.setBigDecimal(4, rating);

                        if (reviewText == null ||
                            reviewText.isBlank()) {

                            stmt.setNull(
                                    5,
                                    java.sql.Types.LONGVARCHAR
                            );

                        } else {

                            stmt.setString(
                                    5,
                                    reviewText
                            );
                        }

                        stmt.executeUpdate();
                    }

                    // -------------------------------------------------
                    // 4. Recalculate expert rating
                    // -------------------------------------------------

                    String aggregateSQL = """
                        SELECT
                            COALESCE(AVG(rating), 0),
                            COUNT(*)
                        FROM expert_reviews
                        WHERE expert_id = ?
                        """;

                    BigDecimal newAverage =
                            BigDecimal.ZERO;

                    int newReviewCount = 0;

                    try (PreparedStatement stmt =
                                 connection.prepareStatement(
                                         aggregateSQL)) {

                        stmt.setLong(1, expertId);

                        try (ResultSet rs =
                                     stmt.executeQuery()) {

                            if (rs.next()) {

                                BigDecimal avg =
                                        rs.getBigDecimal(1);

                                if (avg != null) {
                                    newAverage = avg;
                                }

                                newReviewCount =
                                        rs.getInt(2);
                            }
                        }
                    }

                    // Round rating to 2 decimal places
                    newAverage =
                            newAverage.setScale(
                                    2,
                                    java.math.RoundingMode.HALF_UP
                            );

                    // -------------------------------------------------
                    // 5. Update experts table
                    // -------------------------------------------------

                    String updateExpertSQL = """
                        UPDATE experts
                        SET
                            rating = ?,
                            review_count = ?
                        WHERE expert_id = ?
                        """;

                    try (PreparedStatement stmt =
                                 connection.prepareStatement(
                                         updateExpertSQL)) {

                        stmt.setBigDecimal(
                                1,
                                newAverage
                        );

                        stmt.setInt(
                                2,
                                newReviewCount
                        );

                        stmt.setLong(
                                3,
                                expertId
                        );

                        stmt.executeUpdate();
                    }

                    // -------------------------------------------------
                    // 6. Commit
                    // -------------------------------------------------

                    connection.commit();

                    response.setStatus(
                            HttpServletResponse.SC_CREATED
                    );

                    out.print("{");
                    out.print("\"success\":true,");
                    out.print("\"message\":");
                    out.print(jsonString(
                            "Review submitted successfully."
                    ));
                    out.print(",");
                    out.print("\"rating\":"
                            + rating.toPlainString()
                            + ",");
                    out.print("\"expertRating\":"
                            + newAverage.toPlainString()
                            + ",");
                    out.print("\"reviewCount\":"
                            + newReviewCount);
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
                        "Unable to submit review."
                );
            }
        }
    }

    // =========================================================
    // SESSION HELPERS
    // =========================================================

    private String getSessionRole(HttpSession session) {

        Object role = session.getAttribute("role");

        return role != null
                ? role.toString()
                : null;
    }

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

        // Fallback for student sessions
        Object studentId =
                session.getAttribute("studentId");

        if (studentId instanceof Number) {
            return ((Number) studentId).longValue();
        }

        if (studentId != null) {

            try {
                return Long.parseLong(
                        studentId.toString()
                );
            } catch (NumberFormatException ignored) {
            }
        }

        // Fallback for expert sessions
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
    // FULL NAME
    // =========================================================

    private String buildFullName(String firstName,
                                 String lastName) {

        String first =
                firstName != null
                        ? firstName.trim()
                        : "";

        String last =
                lastName != null
                        ? lastName.trim()
                        : "";

        return (first + " " + last).trim();
    }

    // =========================================================
    // JSON STRING ESCAPER
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
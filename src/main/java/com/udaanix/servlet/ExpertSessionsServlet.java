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

@WebServlet("/api/expert/sessions")
public class ExpertSessionsServlet extends HttpServlet {

    /*
     * ============================================================
     * GET SESSIONS
     * ============================================================
     *
     * GET:
     * /api/expert/sessions
     *
     * Optional:
     *
     * ?type=upcoming
     * ?type=completed
     * ?type=pending
     * ?type=confirmed
     * ?type=cancelled
     * ?type=all
     *
     * Default = all
     */

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            // --------------------------------------------------------
            // CHECK LOGIN SESSION
            // --------------------------------------------------------

            HttpSession session =
                    request.getSession(false);

            if (session == null
                    || session.getAttribute("expertId") == null
                    || !"EXPERT".equals(
                            session.getAttribute("role"))) {

                response.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Expert login required."
                    }
                    """);

                return;
            }

            int expertId =
                    (Integer) session.getAttribute("expertId");

            // --------------------------------------------------------
            // SESSION TYPE
            // --------------------------------------------------------

            String type =
                    request.getParameter("type");

            if (type == null || type.isBlank()) {
                type = "all";
            }

            type = type.trim().toLowerCase();

            // --------------------------------------------------------
            // BUILD SQL
            // --------------------------------------------------------

            StringBuilder sql =
                    new StringBuilder("""
                SELECT
                    eb.booking_id,
                    eb.student_id,
                    eb.expert_id,
                    eb.booking_type,
                    eb.scheduled_at,
                    eb.duration_minutes,
                    eb.amount,
                    eb.status,
                    eb.meeting_link,
                    eb.notes,
                    eb.created_at,
                    eb.updated_at,

                    u.first_name,
                    u.last_name,
                    u.email,
                    u.mobile,
                    u.profile_picture

                FROM expert_bookings eb

                INNER JOIN users u
                    ON eb.student_id = u.user_id

                WHERE eb.expert_id = ?
                """);

            // --------------------------------------------------------
            // FILTER
            // --------------------------------------------------------

            switch (type) {

                case "upcoming":

                    sql.append("""
                        AND eb.scheduled_at IS NOT NULL
                        AND eb.scheduled_at >= NOW()
                        AND eb.status IN ('PENDING', 'CONFIRMED')
                        """);

                    break;

                case "completed":

                    sql.append("""
                        AND eb.status = 'COMPLETED'
                        """);

                    break;

                case "pending":

                    sql.append("""
                        AND eb.status = 'PENDING'
                        """);

                    break;

                case "confirmed":

                    sql.append("""
                        AND eb.status = 'CONFIRMED'
                        """);

                    break;

                case "cancelled":

                    sql.append("""
                        AND eb.status = 'CANCELLED'
                        """);

                    break;

                case "all":

                    // No additional filter.
                    break;

                default:

                    response.setStatus(
                            HttpServletResponse.SC_BAD_REQUEST
                    );

                    out.println("""
                        {
                            "success": false,
                            "message": "Invalid session type."
                        }
                        """);

                    return;
            }

            // --------------------------------------------------------
            // ORDER
            // --------------------------------------------------------

            if ("completed".equals(type)
                    || "cancelled".equals(type)) {

                sql.append("""
                    ORDER BY eb.scheduled_at DESC
                    """);

            } else {

                sql.append("""
                    ORDER BY eb.scheduled_at ASC
                    """);
            }

            // --------------------------------------------------------
            // JSON RESPONSE
            // --------------------------------------------------------

            StringBuilder sessionsJson =
                    new StringBuilder();

            sessionsJson.append("[");

            boolean first = true;

            int totalSessions = 0;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(
                                 sql.toString()
                         )) {

                ps.setInt(1, expertId);

                try (ResultSet rs =
                             ps.executeQuery()) {

                    while (rs.next()) {

                        if (!first) {
                            sessionsJson.append(",");
                        }

                        first = false;

                        totalSessions++;

                        // ------------------------------------------------
                        // STUDENT DATA
                        // ------------------------------------------------

                        String firstName =
                                safe(
                                    rs.getString("first_name")
                                );

                        String lastName =
                                safe(
                                    rs.getString("last_name")
                                );

                        String studentName =
                                (firstName + " " + lastName)
                                .trim();

                        String studentEmail =
                                safe(
                                    rs.getString("email")
                                );

                        String studentMobile =
                                safe(
                                    rs.getString("mobile")
                                );

                        String studentPicture =
                                safe(
                                    rs.getString(
                                        "profile_picture"
                                    )
                                );

                        // ------------------------------------------------
                        // BOOKING DATA
                        // ------------------------------------------------

                        String bookingType =
                                safe(
                                    rs.getString(
                                        "booking_type"
                                    )
                                );

                        String scheduledAt =
                                safe(
                                    rs.getString(
                                        "scheduled_at"
                                    )
                                );

                        String status =
                                safe(
                                    rs.getString("status")
                                );

                        String meetingLink =
                                safe(
                                    rs.getString(
                                        "meeting_link"
                                    )
                                );

                        String notes =
                                safe(
                                    rs.getString("notes")
                                );

                        // ------------------------------------------------
                        // JSON OBJECT
                        // ------------------------------------------------

                        sessionsJson.append("{");

                        sessionsJson.append(
                                "\"bookingId\":"
                        ).append(
                                rs.getInt("booking_id")
                        ).append(",");

                        sessionsJson.append(
                                "\"studentId\":"
                        ).append(
                                rs.getInt("student_id")
                        ).append(",");

                        sessionsJson.append(
                                "\"expertId\":"
                        ).append(
                                rs.getInt("expert_id")
                        ).append(",");

                        sessionsJson.append(
                                "\"bookingType\":\""
                        ).append(
                                json(bookingType)
                        ).append("\",");

                        sessionsJson.append(
                                "\"scheduledAt\":\""
                        ).append(
                                json(scheduledAt)
                        ).append("\",");

                        sessionsJson.append(
                                "\"durationMinutes\":"
                        ).append(
                                rs.getInt(
                                    "duration_minutes"
                                )
                        ).append(",");

                        sessionsJson.append(
                                "\"amount\":"
                        ).append(
                                rs.getDouble("amount")
                        ).append(",");

                        sessionsJson.append(
                                "\"status\":\""
                        ).append(
                                json(status)
                        ).append("\",");

                        sessionsJson.append(
                                "\"meetingLink\":\""
                        ).append(
                                json(meetingLink)
                        ).append("\",");

                        sessionsJson.append(
                                "\"notes\":\""
                        ).append(
                                json(notes)
                        ).append(",");

                        sessionsJson.append(
                                "\"createdAt\":\""
                        ).append(
                                json(
                                    safe(
                                        rs.getString(
                                            "created_at"
                                        )
                                    )
                                )
                        ).append("\",");

                        sessionsJson.append(
                                "\"updatedAt\":\""
                        ).append(
                                json(
                                    safe(
                                        rs.getString(
                                            "updated_at"
                                        )
                                    )
                                )
                        ).append(",");

                        // ------------------------------------------------
                        // STUDENT OBJECT
                        // ------------------------------------------------

                        sessionsJson.append(
                                "\"student\":{"
                        );

                        sessionsJson.append(
                                "\"id\":"
                        ).append(
                                rs.getInt("student_id")
                        ).append(",");

                        sessionsJson.append(
                                "\"name\":\""
                        ).append(
                                json(studentName)
                        ).append("\",");

                        sessionsJson.append(
                                "\"email\":\""
                        ).append(
                                json(studentEmail)
                        ).append("\",");

                        sessionsJson.append(
                                "\"mobile\":\""
                        ).append(
                                json(studentMobile)
                        ).append("\",");

                        sessionsJson.append(
                                "\"profilePicture\":\""
                        ).append(
                                json(studentPicture)
                        ).append("\"");

                        sessionsJson.append("}");

                        sessionsJson.append("}");
                    }
                }

            } catch (Exception e) {

                e.printStackTrace();

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Unable to load expert sessions."
                    }
                    """);

                return;
            }

            sessionsJson.append("]");

            // --------------------------------------------------------
            // FINAL RESPONSE
            // --------------------------------------------------------

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            out.println("{");

            out.println("\"success\":true,");

            out.println(
                    "\"type\":\""
                    + json(type)
                    + "\","
            );

            out.println(
                    "\"totalSessions\":"
                    + totalSessions
                    + ","
            );

            out.println(
                    "\"sessions\":"
                    + sessionsJson
            );

            out.println("}");
        }
    }


    /*
     * ============================================================
     * UPDATE SESSION STATUS
     * ============================================================
     *
     * POST:
     * /api/expert/sessions
     *
     * Parameters:
     *
     * bookingId
     * status
     *
     * Allowed:
     *
     * PENDING
     * CONFIRMED
     * COMPLETED
     * CANCELLED
     */

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            // --------------------------------------------------------
            // CHECK SESSION
            // --------------------------------------------------------

            HttpSession session =
                    request.getSession(false);

            if (session == null
                    || session.getAttribute("expertId") == null
                    || !"EXPERT".equals(
                            session.getAttribute("role"))) {

                response.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Expert login required."
                    }
                    """);

                return;
            }

            int expertId =
                    (Integer) session.getAttribute("expertId");

            // --------------------------------------------------------
            // PARAMETERS
            // --------------------------------------------------------

            String bookingIdParam =
                    request.getParameter("bookingId");

            String newStatus =
                    request.getParameter("status");

            if (bookingIdParam == null
                    || bookingIdParam.isBlank()
                    || newStatus == null
                    || newStatus.isBlank()) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Booking ID and status are required."
                    }
                    """);

                return;
            }

            int bookingId;

            try {

                bookingId =
                        Integer.parseInt(
                                bookingIdParam.trim()
                        );

            } catch (NumberFormatException e) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Invalid booking ID."
                    }
                    """);

                return;
            }

            newStatus =
                    newStatus.trim().toUpperCase();

            // --------------------------------------------------------
            // VALID STATUS
            // --------------------------------------------------------

            if (!newStatus.equals("PENDING")
                    && !newStatus.equals("CONFIRMED")
                    && !newStatus.equals("COMPLETED")
                    && !newStatus.equals("CANCELLED")) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Invalid session status."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // UPDATE
            // --------------------------------------------------------

            String updateSQL = """
                UPDATE expert_bookings
                SET
                    status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE booking_id = ?
                  AND expert_id = ?
                """;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(
                                 updateSQL
                         )) {

                ps.setString(1, newStatus);
                ps.setInt(2, bookingId);
                ps.setInt(3, expertId);

                int updated =
                        ps.executeUpdate();

                if (updated == 0) {

                    response.setStatus(
                            HttpServletResponse.SC_NOT_FOUND
                    );

                    out.println("""
                        {
                            "success": false,
                            "message": "Session not found."
                        }
                        """);

                    return;
                }

            } catch (Exception e) {

                e.printStackTrace();

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Unable to update session status."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // SUCCESS
            // --------------------------------------------------------

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            out.println("""
                {
                    "success": true,
                    "message": "Session status updated successfully."
                }
                """);
        }
    }


    /*
     * ============================================================
     * NULL SAFE
     * ============================================================
     */

    private static String safe(String value) {

        return value == null ? "" : value;
    }


    /*
     * ============================================================
     * JSON ESCAPE
     * ============================================================
     */

    private static String json(String value) {

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
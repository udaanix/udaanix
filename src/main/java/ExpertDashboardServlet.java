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

@WebServlet("/api/expert/dashboard")
public class ExpertDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        /*
         * Prefer the authenticated expertId stored in the session.
         * The optional query parameter is kept for backward compatibility.
         */
        String expertIdParam = request.getParameter("expertId");
        HttpSession session = request.getSession(false);

        try (PrintWriter out = response.getWriter()) {

            Integer expertId = null;

            if (session != null) {
                Object sessionExpertId = session.getAttribute("expertId");

                if (sessionExpertId instanceof Number number) {
                    expertId = number.intValue();
                } else if (sessionExpertId != null) {
                    try {
                        expertId = Integer.parseInt(
                                sessionExpertId.toString()
                        );
                    } catch (NumberFormatException ignored) {
                        // Fall through to the optional query parameter.
                    }
                }
            }

            /*
             * Backward compatibility:
             * /api/expert/dashboard?expertId=123 still works.
             */
            if (expertId == null &&
                expertIdParam != null &&
                !expertIdParam.isBlank()) {

                try {
                    expertId = Integer.parseInt(expertIdParam);
                } catch (NumberFormatException e) {

                    response.setStatus(
                            HttpServletResponse.SC_BAD_REQUEST
                    );

                    out.println("""
                        {
                            "success": false,
                            "message": "Invalid expert ID."
                        }
                        """);

                    return;
                }
            }

            if (expertId == null || expertId <= 0) {

                response.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Expert login session is required."
                    }
                    """);

                return;
            }

            try (Connection connection = DBConnection.getConnection()) {

                /*
                 * ---------------------------------------------------------
                 * 1. GET EXPERT PROFILE
                 * ---------------------------------------------------------
                 */

                String profileSQL = """
                    SELECT
                        e.expert_id,
                        e.age,
                        e.job_title,
                        e.company,
                        e.industry,
                        e.expertise,
                        e.stream,
                        e.primary_career,
                        e.experience_years,
                        e.session_fee,
                        e.session_duration_minutes,
                        e.city,
                        e.linkedin_url,
                        e.intro_video_url,
                        e.profile_picture,
                        e.bio,
                        e.rating,
                        e.review_count,
                        e.students_mentored,
                        e.is_verified,
                        e.availability_status,
                        u.first_name,
                        u.last_name,
                        u.email,
                        u.mobile,
                        u.profile_picture AS user_profile_picture
                    FROM experts e
                    INNER JOIN users u
                        ON e.expert_id = u.user_id
                    WHERE e.expert_id = ?
                      AND u.role = 'EXPERT'
                      AND u.is_active = 1
                    """;

                String firstName = "";
                String lastName = "";
                String email = "";
                String mobile = "";

                String jobTitle = "";
                String company = "";
                String industry = "";
                String expertise = "";
                String stream = "";
                String primaryCareer = "";
                String city = "";
                String linkedinUrl = "";
                String introVideoUrl = "";
                String profilePicture = "";
                String bio = "";
                String availabilityStatus = "Available";

                int age = 0;
                int experienceYears = 0;
                int sessionDuration = 30;
                int reviewCount = 0;
                int studentsMentored = 0;

                double sessionFee = 0.0;
                double rating = 0.0;

                boolean isVerified = false;

                boolean expertFound = false;

                try (PreparedStatement ps =
                             connection.prepareStatement(profileSQL)) {

                    ps.setInt(1, expertId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {

                            expertFound = true;

                            firstName = safe(rs.getString("first_name"));
                            lastName = safe(rs.getString("last_name"));
                            email = safe(rs.getString("email"));
                            mobile = safe(rs.getString("mobile"));

                            jobTitle = safe(rs.getString("job_title"));
                            company = safe(rs.getString("company"));
                            industry = safe(rs.getString("industry"));
                            expertise = safe(rs.getString("expertise"));
                            stream = safe(rs.getString("stream"));
                            primaryCareer = safe(rs.getString("primary_career"));
                            city = safe(rs.getString("city"));

                            linkedinUrl = safe(rs.getString("linkedin_url"));
                            introVideoUrl = safe(rs.getString("intro_video_url"));

                            profilePicture =
                                    safe(rs.getString("profile_picture"));

                            if (profilePicture.isEmpty()) {
                                profilePicture =
                                        safe(rs.getString("user_profile_picture"));
                            }

                            bio = safe(rs.getString("bio"));

                            availabilityStatus =
                                    safe(rs.getString("availability_status"));

                            age = rs.getInt("age");
                            experienceYears =
                                    rs.getInt("experience_years");

                            sessionDuration =
                                    rs.getInt("session_duration_minutes");

                            reviewCount =
                                    rs.getInt("review_count");

                            studentsMentored =
                                    rs.getInt("students_mentored");

                            sessionFee =
                                    rs.getDouble("session_fee");

                            rating =
                                    rs.getDouble("rating");

                            isVerified =
                                    rs.getBoolean("is_verified");
                        }
                    }
                }

                if (!expertFound) {

                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);

                    out.println("""
                        {
                            "success": false,
                            "message": "Expert profile not found."
                        }
                        """);

                    return;
                }

                /*
                 * ---------------------------------------------------------
                 * 2. ACTIVE MENTEES
                 * ---------------------------------------------------------
                 */

                int activeMentees = 0;

                String menteesSQL = """
                    SELECT COUNT(DISTINCT student_id)
                    FROM expert_bookings
                    WHERE expert_id = ?
                      AND status IN ('PENDING', 'CONFIRMED')
                    """;

                try (PreparedStatement ps =
                             connection.prepareStatement(menteesSQL)) {

                    ps.setInt(1, expertId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            activeMentees = rs.getInt(1);
                        }
                    }
                }

                /*
                 * ---------------------------------------------------------
                 * 3. TOTAL EXPERT SESSIONS
                 * ---------------------------------------------------------
                 */

                int totalSessions = 0;

                String sessionsCountSQL = """
                    SELECT COUNT(*)
                    FROM expert_bookings
                    WHERE expert_id = ?
                      AND status <> 'CANCELLED'
                    """;

                try (PreparedStatement ps =
                             connection.prepareStatement(sessionsCountSQL)) {

                    ps.setInt(1, expertId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            totalSessions = rs.getInt(1);
                        }
                    }
                }

                /*
                 * ---------------------------------------------------------
                 * 4. UPCOMING SESSIONS
                 * ---------------------------------------------------------
                 */

                String upcomingSQL = """
                    SELECT
                        eb.booking_id,
                        eb.student_id,
                        eb.booking_type,
                        eb.scheduled_at,
                        eb.duration_minutes,
                        eb.amount,
                        eb.status,
                        eb.meeting_link,
                        eb.notes,
                        u.first_name,
                        u.last_name
                    FROM expert_bookings eb
                    INNER JOIN users u
                        ON eb.student_id = u.user_id
                    WHERE eb.expert_id = ?
                      AND eb.scheduled_at IS NOT NULL
                      AND eb.scheduled_at >= NOW()
                      AND eb.status IN ('PENDING', 'CONFIRMED')
                    ORDER BY eb.scheduled_at ASC
                    LIMIT 10
                    """;

                StringBuilder sessionsJson = new StringBuilder();
                sessionsJson.append("[");

                boolean firstSession = true;

                try (PreparedStatement ps =
                             connection.prepareStatement(upcomingSQL)) {

                    ps.setInt(1, expertId);

                    try (ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {

                            if (!firstSession) {
                                sessionsJson.append(",");
                            }

                            firstSession = false;

                            String studentName =
                                    safe(rs.getString("first_name"))
                                    + " "
                                    + safe(rs.getString("last_name"));

                            String bookingType =
                                    safe(rs.getString("booking_type"));

                            String scheduledAt =
                                    safe(rs.getString("scheduled_at"));

                            String status =
                                    safe(rs.getString("status"));

                            String meetingLink =
                                    safe(rs.getString("meeting_link"));

                            sessionsJson.append("{")
                                    .append("\"bookingId\":")
                                    .append(rs.getInt("booking_id"))
                                    .append(",")

                                    .append("\"studentId\":")
                                    .append(rs.getInt("student_id"))
                                    .append(",")

                                    .append("\"studentName\":\"")
                                    .append(json(studentName))
                                    .append("\",")

                                    .append("\"bookingType\":\"")
                                    .append(json(bookingType))
                                    .append("\",")

                                    .append("\"scheduledAt\":\"")
                                    .append(json(scheduledAt))
                                    .append("\",")

                                    .append("\"durationMinutes\":")
                                    .append(rs.getInt("duration_minutes"))
                                    .append(",")

                                    .append("\"amount\":")
                                    .append(rs.getDouble("amount"))
                                    .append(",")

                                    .append("\"status\":\"")
                                    .append(json(status))
                                    .append("\",")

                                    .append("\"meetingLink\":\"")
                                    .append(json(meetingLink))
                                    .append("\"")

                                    .append("}");
                        }
                    }
                }

                sessionsJson.append("]");

                /*
                 * ---------------------------------------------------------
                 * 5. FINAL JSON RESPONSE
                 * ---------------------------------------------------------
                 */

                response.setStatus(HttpServletResponse.SC_OK);

                out.println("{");

                out.println("\"success\":true,");

                out.println("\"expert\":{");

                out.println("\"expertId\":" + expertId + ",");

                out.println("\"firstName\":\"" + json(firstName) + "\",");
                out.println("\"lastName\":\"" + json(lastName) + "\",");
                out.println("\"email\":\"" + json(email) + "\",");
                out.println("\"mobile\":\"" + json(mobile) + "\",");

                out.println("\"age\":" + age + ",");

                out.println("\"jobTitle\":\"" + json(jobTitle) + "\",");
                out.println("\"company\":\"" + json(company) + "\",");
                out.println("\"industry\":\"" + json(industry) + "\",");
                out.println("\"expertise\":\"" + json(expertise) + "\",");
                out.println("\"stream\":\"" + json(stream) + "\",");
                out.println("\"primaryCareer\":\"" + json(primaryCareer) + "\",");
                out.println("\"experienceYears\":" + experienceYears + ",");

                out.println("\"sessionFee\":" + sessionFee + ",");
                out.println("\"sessionDurationMinutes\":" + sessionDuration + ",");

                out.println("\"city\":\"" + json(city) + "\",");
                out.println("\"linkedinUrl\":\"" + json(linkedinUrl) + "\",");
                out.println("\"introVideoUrl\":\"" + json(introVideoUrl) + "\",");

                out.println("\"profilePicture\":\""
                        + json(profilePicture) + "\",");

                out.println("\"bio\":\"" + json(bio) + "\",");

                out.println("\"rating\":" + rating + ",");
                out.println("\"reviewCount\":" + reviewCount + ",");
                out.println("\"studentsMentored\":" + studentsMentored + ",");

                out.println("\"isVerified\":" + isVerified + ",");

                out.println("\"availabilityStatus\":\""
                        + json(availabilityStatus) + "\"");

                out.println("},");

                out.println("\"stats\":{");

                out.println("\"activeMentees\":"
                        + activeMentees + ",");

                out.println("\"totalSessions\":"
                        + totalSessions + ",");

                out.println("\"studentsMentored\":"
                        + studentsMentored + ",");

                out.println("\"rating\":"
                        + rating);

                out.println("},");

                out.println("\"upcomingSessions\":"
                        + sessionsJson);

                out.println("}");

            } catch (Exception e) {

                e.printStackTrace();

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Unable to load expert dashboard."
                    }
                    """);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

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
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

@WebServlet("/api/expert/profile")
public class ExpertProfileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        HttpSession session = request.getSession(false);
        Integer expertId = getAuthenticatedExpertId(session);

        if (expertId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Expert login required.\"}");
            return;
        }

        String sql = """
            SELECT
                u.user_id,
                u.first_name,
                u.last_name,
                u.email,
                u.mobile,
                u.profile_picture AS user_profile_picture,
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
                e.profile_picture AS expert_profile_picture,
                e.bio,
                e.rating,
                e.review_count,
                e.students_mentored,
                e.is_verified,
                e.availability_status
            FROM users u
            INNER JOIN experts e ON e.expert_id = u.user_id
            WHERE u.user_id = ?
              AND u.role = 'EXPERT'
              AND u.is_active = 1
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, expertId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                            "{\"success\":false,\"message\":\"Expert profile not found.\"}");
                    return;
                }

                String picture = rs.getString("expert_profile_picture");
                if (picture == null || picture.isBlank()) {
                    picture = rs.getString("user_profile_picture");
                }

                String json = "{"
                        + "\"success\":true,"
                        + "\"profile\":{"
                        + "\"expertId\":" + rs.getInt("expert_id") + ","
                        + "\"userId\":" + rs.getInt("user_id") + ","
                        + "\"firstName\":\"" + jsonEscape(rs.getString("first_name")) + "\","
                        + "\"lastName\":\"" + jsonEscape(rs.getString("last_name")) + "\","
                        + "\"email\":\"" + jsonEscape(rs.getString("email")) + "\","
                        + "\"mobile\":\"" + jsonEscape(rs.getString("mobile")) + "\","
                        + "\"age\":" + rs.getInt("age") + ","
                        + "\"jobTitle\":\"" + jsonEscape(rs.getString("job_title")) + "\","
                        + "\"company\":\"" + jsonEscape(rs.getString("company")) + "\","
                        + "\"industry\":\"" + jsonEscape(rs.getString("industry")) + "\","
                        + "\"expertise\":\"" + jsonEscape(rs.getString("expertise")) + "\","
                        + "\"stream\":\"" + jsonEscape(rs.getString("stream")) + "\","
                        + "\"primaryCareer\":\"" + jsonEscape(rs.getString("primary_career")) + "\","
                        + "\"experienceYears\":" + rs.getInt("experience_years") + ","
                        + "\"sessionFee\":" + rs.getDouble("session_fee") + ","
                        + "\"sessionDurationMinutes\":" + rs.getInt("session_duration_minutes") + ","
                        + "\"city\":\"" + jsonEscape(rs.getString("city")) + "\","
                        + "\"linkedinUrl\":\"" + jsonEscape(rs.getString("linkedin_url")) + "\","
                        + "\"introVideoUrl\":\"" + jsonEscape(rs.getString("intro_video_url")) + "\","
                        + "\"profilePicture\":\"" + jsonEscape(picture) + "\","
                        + "\"bio\":\"" + jsonEscape(rs.getString("bio")) + "\","
                        + "\"rating\":" + rs.getDouble("rating") + ","
                        + "\"reviewCount\":" + rs.getInt("review_count") + ","
                        + "\"studentsMentored\":" + rs.getInt("students_mentored") + ","
                        + "\"isVerified\":" + rs.getBoolean("is_verified") + ","
                        + "\"availabilityStatus\":\"" + jsonEscape(rs.getString("availability_status")) + "\""
                        + "}}";

                writeJson(response, HttpServletResponse.SC_OK, json);
            }
        } catch (Exception e) {
            log("Failed to load expert profile for expertId=" + expertId, e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Unable to load expert profile. Check the server log.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");

        HttpSession session = request.getSession(false);
        Integer expertId = getAuthenticatedExpertId(session);

        if (expertId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Expert login required.\"}");
            return;
        }

        String firstName = param(request, "firstName");
        String lastName = param(request, "lastName");
        String mobile = param(request, "mobile");

        String ageParam = param(request, "age");
        String jobTitle = param(request, "jobTitle");
        String company = param(request, "company");
        String industry = param(request, "industry");
        String expertise = param(request, "expertise");
        String stream = param(request, "stream");
        String primaryCareer = param(request, "primaryCareer");
        String experienceParam = param(request, "experienceYears");
        String sessionFeeParam = param(request, "sessionFee");
        String sessionDurationParam = param(request, "sessionDurationMinutes");
        String city = param(request, "city");
        String linkedinUrl = param(request, "linkedinUrl");
        String introVideoUrl = param(request, "introVideoUrl");
        String bio = param(request, "bio");
        String availabilityStatus = param(request, "availabilityStatus");

        if (firstName.isBlank() || lastName.isBlank()) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"First name and last name are required.\"}");
            return;
        }

        final int age;
        final int experienceYears;
        final int sessionDuration;
        final double sessionFee;

        try {
            age = parseInt(ageParam, 0, "Age");
            experienceYears = parseInt(experienceParam, 0, "Experience");
            sessionDuration = parseInt(sessionDurationParam, 30, "Session duration");
            sessionFee = parseDouble(sessionFeeParam, 0.0, "Session fee");
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"" + jsonEscape(e.getMessage()) + "\"}");
            return;
        }

        if (age < 0 || age > 120) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Please enter a valid age.\"}");
            return;
        }
        if (experienceYears < 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Experience cannot be negative.\"}");
            return;
        }
        if (sessionFee < 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Session fee cannot be negative.\"}");
            return;
        }
        if (sessionDuration <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":\"Session duration must be greater than zero.\"}");
            return;
        }

        if (availabilityStatus.isBlank()) {
            availabilityStatus = "Available";
        }

        String updateUserSql = """
            UPDATE users
            SET first_name = ?,
                last_name = ?,
                mobile = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ?
              AND role = 'EXPERT'
            """;

        String updateExpertSql = """
            UPDATE experts
            SET age = ?,
                job_title = ?,
                company = ?,
                industry = ?,
                expertise = ?,
                stream = ?,
                primary_career = ?,
                experience_years = ?,
                session_fee = ?,
                session_duration_minutes = ?,
                city = ?,
                linkedin_url = ?,
                intro_video_url = ?,
                bio = ?,
                availability_status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE expert_id = ?
            """;

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int userRows;
                try (PreparedStatement ps = connection.prepareStatement(updateUserSql)) {
                    ps.setString(1, firstName);
                    ps.setString(2, lastName);
                    ps.setString(3, mobile);
                    ps.setInt(4, expertId);
                    userRows = ps.executeUpdate();
                }

                int expertRows;
                try (PreparedStatement ps = connection.prepareStatement(updateExpertSql)) {
                    ps.setInt(1, age);
                    ps.setString(2, jobTitle);
                    ps.setString(3, company);
                    ps.setString(4, industry);
                    ps.setString(5, expertise);
                    ps.setString(6, stream);
                    ps.setString(7, primaryCareer);
                    ps.setInt(8, experienceYears);
                    ps.setDouble(9, sessionFee);
                    ps.setInt(10, sessionDuration);
                    ps.setString(11, city);
                    ps.setString(12, linkedinUrl);
                    ps.setString(13, introVideoUrl);
                    ps.setString(14, bio);
                    ps.setString(15, availabilityStatus);
                    ps.setInt(16, expertId);
                    expertRows = ps.executeUpdate();
                }

                if (userRows != 1 || expertRows != 1) {
                    connection.rollback();
                    writeJson(response, HttpServletResponse.SC_CONFLICT,
                            "{\"success\":false,\"message\":\"Profile was not saved. Matching user/expert records were not found.\"}");
                    return;
                }

                connection.commit();

                session.setAttribute("expertName", (firstName + " " + lastName).trim());

                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"message\":\"Expert profile updated successfully.\"}");

            } catch (Exception e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Connection is closing; do not mask the original result.
                }
            }
        } catch (Exception e) {
            log("Failed to update expert profile for expertId=" + expertId, e);
            if (!response.isCommitted()) {
                writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "{\"success\":false,\"message\":\"Unable to update expert profile. Check the GlassFish server log.\"}");
            }
        }
    }

    private static Integer getAuthenticatedExpertId(HttpSession session) {
        if (session == null
                || !(session.getAttribute("expertId") instanceof Integer id)
                || !"EXPERT".equals(session.getAttribute("role"))) {
            return null;
        }
        return id;
    }

    private static String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private static int parseInt(String value, int defaultValue, String fieldName) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    private static double parseDouble(String value, double defaultValue, String fieldName) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed)) {
                throw new NumberFormatException("Not finite");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }
        return result.toString();
    }

    private static void writeJson(HttpServletResponse response, int status, String json)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentLength(-1);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
            writer.flush();
        }
    }
}

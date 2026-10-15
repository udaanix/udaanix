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

@WebServlet("/api/auth/expert-login")
public class ExpertLoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try (PrintWriter out = response.getWriter()) {

            // ---------------------------------------------------------
            // VALIDATION
            // ---------------------------------------------------------

            if (email == null || password == null
                    || email.isBlank()
                    || password.isBlank()) {

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                out.println("""
                    {
                        "success": false,
                        "message": "Email and password are required."
                    }
                    """);

                return;
            }

            email = email.trim().toLowerCase();

            // ---------------------------------------------------------
            // FIND EXPERT
            // ---------------------------------------------------------

            String sql = """
                SELECT
                    u.user_id,
                    u.first_name,
                    u.last_name,
                    u.email,
                    u.mobile,
                    u.password_hash,
                    u.profile_picture AS user_profile_picture,
                    u.is_active,

                    e.expert_id,
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

                INNER JOIN experts e
                    ON e.expert_id = u.user_id

                WHERE u.email = ?
                  AND u.role = 'EXPERT'

                LIMIT 1
                """;

            try (Connection connection = DBConnection.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {

                    // -------------------------------------------------
                    // EXPERT NOT FOUND
                    // -------------------------------------------------

                    if (!rs.next()) {

                        response.setStatus(
                                HttpServletResponse.SC_UNAUTHORIZED
                        );

                        out.println("""
                            {
                                "success": false,
                                "message": "Invalid expert email or password."
                            }
                            """);

                        return;
                    }

                    // -------------------------------------------------
                    // ACCOUNT ACTIVE CHECK
                    // -------------------------------------------------

                    boolean isActive = rs.getBoolean("is_active");

                    if (!isActive) {

                        response.setStatus(
                                HttpServletResponse.SC_FORBIDDEN
                        );

                        out.println("""
                            {
                                "success": false,
                                "message": "Your expert account is inactive. Please contact Udaanix support."
                            }
                            """);

                        return;
                    }

                    // -------------------------------------------------
                    // PASSWORD CHECK
                    // -------------------------------------------------

                    String passwordHash =
                            rs.getString("password_hash");

                    if (passwordHash == null
                            || passwordHash.isBlank()
                            || !BCrypt.checkpw(password, passwordHash)) {

                        response.setStatus(
                                HttpServletResponse.SC_UNAUTHORIZED
                        );

                        out.println("""
                            {
                                "success": false,
                                "message": "Invalid expert email or password."
                            }
                            """);

                        return;
                    }

                    // -------------------------------------------------
                    // GET EXPERT DATA
                    // -------------------------------------------------

                    int userId =
                            rs.getInt("user_id");

                    int expertId =
                            rs.getInt("expert_id");

                    String firstName =
                            safe(rs.getString("first_name"));

                    String lastName =
                            safe(rs.getString("last_name"));

                    String fullName =
                            (firstName + " " + lastName).trim();

                    String expertPicture =
                            safe(rs.getString("expert_profile_picture"));

                    String userPicture =
                            safe(rs.getString("user_profile_picture"));

                    if (expertPicture.isEmpty()) {
                        expertPicture = userPicture;
                    }

                    // -------------------------------------------------
                    // CREATE SESSION
                    // -------------------------------------------------

                    HttpSession session =
                            request.getSession(true);

                    session.setAttribute(
                            "userId",
                            userId
                    );

                    session.setAttribute(
                            "expertId",
                            expertId
                    );

                    session.setAttribute(
                            "role",
                            "EXPERT"
                    );

                    session.setAttribute(
                            "expertName",
                            fullName
                    );

                    session.setAttribute(
                            "expertEmail",
                            rs.getString("email")
                    );

                    session.setAttribute(
                            "expertProfilePicture",
                            expertPicture
                    );

                    // Session timeout = 30 minutes
                    session.setMaxInactiveInterval(30 * 60);

                    // -------------------------------------------------
                    // SUCCESS RESPONSE
                    // -------------------------------------------------

                    response.setStatus(
                            HttpServletResponse.SC_OK
                    );

                    out.println("{");

                    out.println("\"success\":true,");

                    out.println("\"message\":\"Expert login successful.\",");

                    out.println("\"expert\":{");

                    out.println("\"userId\":"
                            + userId + ",");

                    out.println("\"expertId\":"
                            + expertId + ",");

                    out.println("\"firstName\":\""
                            + json(firstName) + "\",");

                    out.println("\"lastName\":\""
                            + json(lastName) + "\",");

                    out.println("\"fullName\":\""
                            + json(fullName) + "\",");

                    out.println("\"email\":\""
                            + json(rs.getString("email")) + "\",");

                    out.println("\"mobile\":\""
                            + json(safe(rs.getString("mobile")))
                            + "\",");

                    out.println("\"jobTitle\":\""
                            + json(safe(rs.getString("job_title")))
                            + "\",");

                    out.println("\"company\":\""
                            + json(safe(rs.getString("company")))
                            + "\",");

                    out.println("\"industry\":\""
                            + json(safe(rs.getString("industry")))
                            + "\",");

                    out.println("\"profilePicture\":\""
                            + json(expertPicture)
                            + "\",");

                    out.println("\"isVerified\":"
                            + rs.getBoolean("is_verified"));

                    out.println("}");

                    out.println("}");
                }

            } catch (Exception e) {

                e.printStackTrace();

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Login failed. Please try again."
                    }
                    """);
            }
        }
    }

    // -------------------------------------------------------------
    // NULL-SAFE STRING
    // -------------------------------------------------------------

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    // -------------------------------------------------------------
    // JSON ESCAPE
    // -------------------------------------------------------------

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
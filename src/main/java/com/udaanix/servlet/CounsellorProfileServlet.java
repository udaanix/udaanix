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

@WebServlet("/api/counsellor/profile")
public class CounsellorProfileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // GET PROFILE
    // =========================================================

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        HttpSession session =
                request.getSession(false);

        Long userId =
                getLoggedInUserId(session);

        // -----------------------------------------------------
        // LOGIN CHECK
        // -----------------------------------------------------

        if (userId == null) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Counsellor is not logged in."
            );

            return;
        }

        // -----------------------------------------------------
        // ROLE CHECK
        // -----------------------------------------------------

        String role =
                session != null
                        ? (String) session.getAttribute("role")
                        : null;

        if (role == null ||
                !"COUNSELLOR".equalsIgnoreCase(role)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Access denied. Counsellor account required."
            );

            return;
        }

        // =====================================================
        // DATABASE QUERY
        // =====================================================

        String sql = """
            SELECT
                u.user_id,
                u.role,
                u.first_name,
                u.last_name,
                u.email,
                u.mobile,
                u.profile_picture AS user_profile_picture,
                u.is_active,

                c.qualification,
                c.specialization,
                c.experience_years,
                c.city,
                c.bio,
                c.profile_picture AS counsellor_profile_picture,
                c.is_verified

            FROM users u

            INNER JOIN counsellors c
                ON u.user_id = c.counsellor_id

            WHERE u.user_id = ?
              AND u.role = 'COUNSELLOR'
            """;

        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setLong(1, userId);

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                // -------------------------------------------------
                // PROFILE NOT FOUND
                // -------------------------------------------------

                if (!rs.next()) {

                    sendError(
                            response,
                            out,
                            HttpServletResponse.SC_NOT_FOUND,
                            "Counsellor profile not found."
                    );

                    return;
                }

                // -------------------------------------------------
                // USER DATA
                // -------------------------------------------------

                long id =
                        rs.getLong("user_id");

                String roleValue =
                        safe(rs.getString("role"));

                String firstName =
                        safe(rs.getString("first_name"));

                String lastName =
                        safe(rs.getString("last_name"));

                String email =
                        safe(rs.getString("email"));

                String mobile =
                        safe(rs.getString("mobile"));

                String userProfilePicture =
                        safe(
                                rs.getString(
                                        "user_profile_picture"
                                )
                        );

                boolean isActive =
                        rs.getBoolean("is_active");

                // -------------------------------------------------
                // COUNSELLOR DATA
                // -------------------------------------------------

                String qualification =
                        safe(
                                rs.getString(
                                        "qualification"
                                )
                        );

                String specialization =
                        safe(
                                rs.getString(
                                        "specialization"
                                )
                        );

                int experienceYears =
                        rs.getInt(
                                "experience_years"
                        );

                String city =
                        safe(
                                rs.getString("city")
                        );

                String bio =
                        safe(
                                rs.getString("bio")
                        );

                String counsellorProfilePicture =
                        safe(
                                rs.getString(
                                        "counsellor_profile_picture"
                                )
                        );

                boolean isVerified =
                        rs.getBoolean(
                                "is_verified"
                        );

                // -------------------------------------------------
                // PROFILE PICTURE
                // users.profile_picture is primary.
                // counsellors.profile_picture is fallback.
                // -------------------------------------------------

                String profilePicture =
                        !userProfilePicture.isBlank()
                                ? userProfilePicture
                                : counsellorProfilePicture;

                String profilePictureUrl = "";

                if (!profilePicture.isBlank()) {

                    profilePictureUrl =
                            request.getContextPath()
                            + "/api/counsellor/profile-picture";
                }

                // =================================================
                // JSON RESPONSE
                // =================================================

                out.print("{");

                out.print("\"success\":true,");

                out.print("\"counsellor\":{");

                out.print(
                        "\"userId\":"
                        + id
                        + ","
                );

                out.print(
                        "\"role\":\""
                        + escapeJson(roleValue)
                        + "\","
                );

                out.print(
                        "\"firstName\":\""
                        + escapeJson(firstName)
                        + "\","
                );

                out.print(
                        "\"lastName\":\""
                        + escapeJson(lastName)
                        + "\","
                );

                out.print(
                        "\"email\":\""
                        + escapeJson(email)
                        + "\","
                );

                out.print(
                        "\"mobile\":\""
                        + escapeJson(mobile)
                        + "\","
                );

                out.print(
                        "\"profilePicture\":\""
                        + escapeJson(profilePictureUrl)
                        + "\","
                );

                out.print(
                        "\"qualification\":\""
                        + escapeJson(qualification)
                        + "\","
                );

                out.print(
                        "\"specialization\":\""
                        + escapeJson(specialization)
                        + "\","
                );

                out.print(
                        "\"experienceYears\":"
                        + experienceYears
                        + ","
                );

                out.print(
                        "\"city\":\""
                        + escapeJson(city)
                        + "\","
                );

                out.print(
                        "\"bio\":\""
                        + escapeJson(bio)
                        + "\","
                );

                out.print(
                        "\"isVerified\":"
                        + isVerified
                        + ","
                );

                out.print(
                        "\"isActive\":"
                        + isActive
                );

                out.print("}");

                out.print("}");
            }

        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load counsellor profile."
            );
        }
    }


    // =========================================================
    // POST
    // =========================================================

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        updateProfile(request, response);
    }


    // =========================================================
    // PUT
    // =========================================================

    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        updateProfile(request, response);
    }


    // =========================================================
    // UPDATE PROFILE
    // =========================================================

    private void updateProfile(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out =
                response.getWriter();

        HttpSession session =
                request.getSession(false);

        Long userId =
                getLoggedInUserId(session);

        // -----------------------------------------------------
        // LOGIN CHECK
        // -----------------------------------------------------

        if (userId == null) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Counsellor is not logged in."
            );

            return;
        }

        // -----------------------------------------------------
        // ROLE CHECK
        // -----------------------------------------------------

        String role =
                session != null
                        ? (String) session.getAttribute("role")
                        : null;

        if (role == null ||
                !"COUNSELLOR".equalsIgnoreCase(role)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Access denied. Counsellor account required."
            );

            return;
        }

        // =====================================================
        // GET FORM PARAMETERS
        // =====================================================

        String firstName =
                trim(
                        request.getParameter(
                                "firstName"
                        )
                );

        String lastName =
                trim(
                        request.getParameter(
                                "lastName"
                        )
                );

        String email =
                trim(
                        request.getParameter(
                                "email"
                        )
                );

        String mobile =
                trim(
                        request.getParameter(
                                "mobile"
                        )
                );

        String qualification =
                trim(
                        request.getParameter(
                                "qualification"
                        )
                );

        String specialization =
                trim(
                        request.getParameter(
                                "specialization"
                        )
                );

        String experience =
                trim(
                        request.getParameter(
                                "experienceYears"
                        )
                );

        String city =
                trim(
                        request.getParameter(
                                "city"
                        )
                );

        String bio =
                trim(
                        request.getParameter(
                                "bio"
                        )
                );

        // =====================================================
        // VALIDATION
        // =====================================================

        if (firstName.isEmpty()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "First name is required."
            );

            return;
        }

        if (lastName.isEmpty()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Last name is required."
            );

            return;
        }

        if (email.isEmpty()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Email is required."
            );

            return;
        }

        if (!email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Please enter a valid email address."
            );

            return;
        }

        if (qualification.isEmpty()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Qualification is required."
            );

            return;
        }

        if (specialization.isEmpty()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Specialization is required."
            );

            return;
        }

        // =====================================================
        // EXPERIENCE
        // =====================================================

        int experienceYears;

        try {

            experienceYears =
                    experience.isEmpty()
                            ? 0
                            : Integer.parseInt(
                                    experience
                            );

            if (experienceYears < 0) {
                experienceYears = 0;
            }

        } catch (NumberFormatException e) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Experience must be a valid number."
            );

            return;
        }

        email =
                email.toLowerCase();

        Connection con = null;

        try {

            con =
                    DBConnection.getConnection();

            con.setAutoCommit(false);

            // =================================================
            // DUPLICATE EMAIL CHECK
            // =================================================

            String duplicateSql = """
                SELECT user_id
                FROM users
                WHERE LOWER(email) = ?
                  AND user_id <> ?
                """;

            try (
                    PreparedStatement ps =
                            con.prepareStatement(
                                    duplicateSql
                            )
            ) {

                ps.setString(
                        1,
                        email
                );

                ps.setLong(
                        2,
                        userId
                );

                try (
                        ResultSet rs =
                                ps.executeQuery()
                ) {

                    if (rs.next()) {

                        con.rollback();

                        sendError(
                                response,
                                out,
                                HttpServletResponse.SC_CONFLICT,
                                "This email is already registered with another account."
                        );

                        return;
                    }
                }
            }

            // =================================================
            // UPDATE USERS
            // =================================================

            String updateUsersSql = """
                UPDATE users
                SET
                    first_name = ?,
                    last_name = ?,
                    email = ?,
                    mobile = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND role = 'COUNSELLOR'
                """;

            int usersUpdated;

            try (
                    PreparedStatement ps =
                            con.prepareStatement(
                                    updateUsersSql
                            )
            ) {

                ps.setString(
                        1,
                        firstName
                );

                ps.setString(
                        2,
                        lastName
                );

                ps.setString(
                        3,
                        email
                );

                ps.setString(
                        4,
                        mobile
                );

                ps.setLong(
                        5,
                        userId
                );

                usersUpdated =
                        ps.executeUpdate();
            }

            if (usersUpdated == 0) {

                con.rollback();

                sendError(
                        response,
                        out,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Counsellor account not found."
                );

                return;
            }

            // =================================================
            // UPDATE COUNSELLORS
            // =================================================

            String updateCounsellorSql = """
                UPDATE counsellors
                SET
                    qualification = ?,
                    specialization = ?,
                    experience_years = ?,
                    city = ?,
                    bio = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE counsellor_id = ?
                """;

            int counsellorUpdated;

            try (
                    PreparedStatement ps =
                            con.prepareStatement(
                                    updateCounsellorSql
                            )
            ) {

                ps.setString(
                        1,
                        qualification
                );

                ps.setString(
                        2,
                        specialization
                );

                ps.setInt(
                        3,
                        experienceYears
                );

                ps.setString(
                        4,
                        city
                );

                ps.setString(
                        5,
                        bio
                );

                ps.setLong(
                        6,
                        userId
                );

                counsellorUpdated =
                        ps.executeUpdate();
            }

            if (counsellorUpdated == 0) {

                con.rollback();

                sendError(
                        response,
                        out,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Counsellor profile record not found."
                );

                return;
            }

            // =================================================
            // COMMIT
            // =================================================

            con.commit();

            // =================================================
            // UPDATE SESSION
            // =================================================

            session.setAttribute(
                    "firstName",
                    firstName
            );

            session.setAttribute(
                    "lastName",
                    lastName
            );

            session.setAttribute(
                    "email",
                    email
            );

            session.setAttribute(
                    "mobile",
                    mobile
            );

            // =================================================
            // SUCCESS
            // =================================================

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            out.print(
                    "{"
                    + "\"success\":true,"
                    + "\"message\":\"Counsellor profile updated successfully.\""
                    + "}"
            );

        } catch (Exception e) {

            e.printStackTrace();

            if (con != null) {

                try {
                    con.rollback();
                } catch (Exception rollbackError) {
                    rollbackError.printStackTrace();
                }
            }

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to update counsellor profile."
            );

        } finally {

            if (con != null) {

                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // =========================================================
    // GET LOGGED-IN USER ID
    // =========================================================

    private Long getLoggedInUserId(
            HttpSession session) {

        if (session == null) {
            return null;
        }

        Object value =
                session.getAttribute(
                        "userId"
                );

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        return null;
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }


    // =========================================================
    // TRIM
    // =========================================================

    private String trim(String value) {

        return value == null
                ? ""
                : value.trim();
    }


    // =========================================================
    // ERROR RESPONSE
    // =========================================================

    private void sendError(
            HttpServletResponse response,
            PrintWriter out,
            int status,
            String message) {

        response.setStatus(status);

        out.print(
                "{"
                + "\"success\":false,"
                + "\"message\":\""
                + escapeJson(message)
                + "\""
                + "}"
        );
    }


    // =========================================================
    // JSON ESCAPE
    // =========================================================

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
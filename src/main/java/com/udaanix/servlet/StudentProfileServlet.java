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

/**
 * Student Profile Servlet
 *
 * GET  /api/student/profile
 *      -> Logged-in student's profile fetches from database
 *
 * PUT  /api/student/profile
 *      -> Logged-in student's profile updates in database
 *
 * Profile picture is handled by:
 *      /api/student/profile-picture
 */
@WebServlet("/api/student/profile")
public class StudentProfileServlet extends HttpServlet {

    /* =========================================================
       GET STUDENT PROFILE
       ========================================================= */

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        /* -----------------------------------------------------
           GET EXISTING SESSION
           ----------------------------------------------------- */

        HttpSession session =
                request.getSession(false);


        /* -----------------------------------------------------
           CHECK LOGIN
           ----------------------------------------------------- */

        Long userId =
                getLoggedInUserId(session);


        if (userId == null) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Student is not logged in."
            );

            return;
        }


        /* -----------------------------------------------------
           CHECK ROLE
           ----------------------------------------------------- */

        String role =
                session != null
                        ? (String) session.getAttribute("role")
                        : null;


        if (role != null &&
            !"STUDENT".equalsIgnoreCase(role)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Access denied."
            );

            return;
        }


        /* -----------------------------------------------------
           SQL
           ----------------------------------------------------- */

        String sql = """
            SELECT
                u.user_id,
                u.role,
                u.first_name,
                u.last_name,
                u.email,
                u.mobile,
                u.profile_picture,
                u.is_active,

                s.education_level,
                s.qualification,
                s.bio,
                s.city

            FROM users u

            INNER JOIN students s
                ON u.user_id = s.student_id

            WHERE u.user_id = ?
              AND u.role = 'STUDENT'
            """;


        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setLong(1, userId);


            try (ResultSet rs =
                         ps.executeQuery()) {


                /* -------------------------------------------------
                   PROFILE NOT FOUND
                   ------------------------------------------------- */

                if (!rs.next()) {

                    sendError(
                            response,
                            out,
                            HttpServletResponse.SC_NOT_FOUND,
                            "Student profile not found."
                    );

                    return;
                }


                /* -------------------------------------------------
                   READ USER DATA
                   ------------------------------------------------- */

                long id =
                        rs.getLong("user_id");


                String dbRole =
                        safe(rs.getString("role"));


                String firstName =
                        safe(rs.getString("first_name"));


                String lastName =
                        safe(rs.getString("last_name"));


                String email =
                        safe(rs.getString("email"));


                String mobile =
                        safe(rs.getString("mobile"));


                String profilePicture =
                        safe(rs.getString("profile_picture"));


                String educationLevel =
                        safe(rs.getString("education_level"));


                String qualification =
                        safe(rs.getString("qualification"));


                String bio =
                        safe(rs.getString("bio"));


                String city =
                        safe(rs.getString("city"));


                /* =================================================
                   PROFILE PICTURE URL

                   Database stores the filename.

                   Example:
                   student_12_abc123.jpg

                   Dashboard should NOT try to load this filename
                   directly.

                   Instead, image is served through:
                   /api/student/profile-picture
                   ================================================= */

                String profilePictureUrl = "";

                if (!profilePicture.isBlank()) {

                    profilePictureUrl =
                            request.getContextPath()
                            + "/api/student/profile-picture";
                }


                /* -------------------------------------------------
                   JSON RESPONSE
                   ------------------------------------------------- */

                out.print("{");

                out.print("\"success\":true,");

                out.print("\"student\":{");


                /* USER ID */

                out.print(
                        "\"userId\":" +
                        id +
                        ","
                );


                /* ROLE */

                out.print(
                        "\"role\":\"" +
                        escapeJson(dbRole) +
                        "\","
                );


                /* FIRST NAME */

                out.print(
                        "\"firstName\":\"" +
                        escapeJson(firstName) +
                        "\","
                );


                /* LAST NAME */

                out.print(
                        "\"lastName\":\"" +
                        escapeJson(lastName) +
                        "\","
                );


                /* EMAIL */

                out.print(
                        "\"email\":\"" +
                        escapeJson(email) +
                        "\","
                );


                /* MOBILE */

                out.print(
                        "\"mobile\":\"" +
                        escapeJson(mobile) +
                        "\","
                );


                /* PROFILE PICTURE URL */

                out.print(
                        "\"profilePicture\":\"" +
                        escapeJson(profilePictureUrl) +
                        "\","
                );


                /* EDUCATION LEVEL */

                out.print(
                        "\"educationLevel\":\"" +
                        escapeJson(educationLevel) +
                        "\","
                );


                /* QUALIFICATION */

                out.print(
                        "\"qualification\":\"" +
                        escapeJson(qualification) +
                        "\","
                );


                /* BIO */

                out.print(
                        "\"bio\":\"" +
                        escapeJson(bio) +
                        "\","
                );


                /* CITY */

                out.print(
                        "\"city\":\"" +
                        escapeJson(city) +
                        "\""
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
                    "Unable to load student profile."
            );
        }
    }


    /* =========================================================
       UPDATE STUDENT PROFILE
       ========================================================= */

    /**
     * POST /api/student/profile
     *
     * Profile form updates use POST because application/x-www-form-urlencoded
     * request bodies are reliably parsed by the current GlassFish setup.
     *
     * We still keep doPut() below so the endpoint remains backward compatible.
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        doPut(request, response);
    }


    @Override
    protected void doPut(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();


        /* -----------------------------------------------------
           GET SESSION
           ----------------------------------------------------- */

        HttpSession session =
                request.getSession(false);


        Long userId =
                getLoggedInUserId(session);


        /* -----------------------------------------------------
           CHECK LOGIN
           ----------------------------------------------------- */

        if (userId == null) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Student is not logged in."
            );

            return;
        }


        /* -----------------------------------------------------
           CHECK ROLE
           ----------------------------------------------------- */

        String role =
                session != null
                        ? (String) session.getAttribute("role")
                        : null;


        if (role != null &&
            !"STUDENT".equalsIgnoreCase(role)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Access denied."
            );

            return;
        }


        /* =====================================================
           GET FORM DATA
           ===================================================== */

        String firstName =
                trim(request.getParameter("firstName"));


        String lastName =
                trim(request.getParameter("lastName"));


        String email =
                trim(request.getParameter("email"));


        String mobile =
                trim(request.getParameter("mobile"));


        String educationLevel =
                trim(request.getParameter("educationLevel"));


        String qualification =
                trim(request.getParameter("qualification"));


        String city =
                trim(request.getParameter("city"));


        String bio =
                trim(request.getParameter("bio"));


        /* =====================================================
           VALIDATION
           ===================================================== */

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


        if (educationLevel.isEmpty()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Education level is required."
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


        /* =====================================================
           NORMALIZE EMAIL
           ===================================================== */

        email =
                email.toLowerCase();


        /* =====================================================
           DATABASE CONNECTION
           ===================================================== */

        Connection con = null;


        try {

            con =
                    DBConnection.getConnection();

            con.setAutoCommit(false);


            /* =================================================
               CHECK DUPLICATE EMAIL

               Same student's existing email is allowed.
               Another user's email is not allowed.
               ================================================= */

            String duplicateEmailSql = """
                SELECT user_id
                FROM users
                WHERE LOWER(email) = ?
                  AND user_id <> ?
                """;


            try (
                    PreparedStatement duplicatePs =
                            con.prepareStatement(
                                    duplicateEmailSql
                            )
            ) {

                duplicatePs.setString(
                        1,
                        email
                );


                duplicatePs.setLong(
                        2,
                        userId
                );


                try (
                        ResultSet rs =
                                duplicatePs.executeQuery()
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


            /* =================================================
               UPDATE USERS TABLE
               ================================================= */

            String updateUsersSql = """
                UPDATE users
                SET
                    first_name = ?,
                    last_name = ?,
                    email = ?,
                    mobile = ?
                WHERE user_id = ?
                  AND role = 'STUDENT'
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


            /* =================================================
               CHECK USER UPDATE
               ================================================= */

            if (usersUpdated == 0) {

                con.rollback();

                sendError(
                        response,
                        out,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Student account not found."
                );

                return;
            }


            /* =================================================
               UPDATE STUDENTS TABLE
               ================================================= */

            String updateStudentSql = """
                UPDATE students
                SET
                    education_level = ?,
                    qualification = ?,
                    bio = ?,
                    city = ?
                WHERE student_id = ?
                """;


            int studentUpdated;


            try (
                    PreparedStatement ps =
                            con.prepareStatement(
                                    updateStudentSql
                            )
            ) {

                ps.setString(
                        1,
                        educationLevel
                );


                ps.setString(
                        2,
                        qualification
                );


                ps.setString(
                        3,
                        bio
                );


                ps.setString(
                        4,
                        city
                );


                ps.setLong(
                        5,
                        userId
                );


                studentUpdated =
                        ps.executeUpdate();
            }


            /* =================================================
               CHECK STUDENT UPDATE
               ================================================= */

            if (studentUpdated == 0) {

                con.rollback();

                sendError(
                        response,
                        out,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Student profile record not found."
                );

                return;
            }


            /* =================================================
               COMMIT
               ================================================= */

            con.commit();


            /* =================================================
               UPDATE SESSION DATA

               This makes the current session immediately
               reflect the new name/email.
               ================================================= */

            if (session != null) {

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
            }


            /* =================================================
               SUCCESS RESPONSE
               ================================================= */

            out.print("""
                {
                    "success": true,
                    "message": "Profile updated successfully."
                }
                """);


        } catch (Exception e) {

            e.printStackTrace();


            /* =================================================
               ROLLBACK
               ================================================= */

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
                    "Unable to update student profile."
            );


        } finally {

            /* =================================================
               CLOSE CONNECTION
               ================================================= */

            if (con != null) {

                try {

                    con.setAutoCommit(true);

                    con.close();

                } catch (Exception closeError) {

                    closeError.printStackTrace();
                }
            }
        }
    }


    /* =========================================================
       GET LOGGED-IN USER ID
       ========================================================= */

    private Long getLoggedInUserId(
            HttpSession session) {

        if (session == null) {
            return null;
        }


        Object value =
                session.getAttribute("userId");


        /*
         * Important:
         * Session userId can be Integer, Long,
         * or another Number implementation.
         *
         * Therefore do not cast directly to Integer.
         */

        if (!(value instanceof Number)) {
            return null;
        }


        return ((Number) value).longValue();
    }


    /* =========================================================
       NULL-SAFE STRING
       ========================================================= */

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }


    /* =========================================================
       TRIM NULL-SAFE PARAMETER
       ========================================================= */

    private String trim(String value) {

        return value == null
                ? ""
                : value.trim();
    }


    /* =========================================================
       JSON ESCAPE
       ========================================================= */

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }


        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    /* =========================================================
       COMMON ERROR RESPONSE
       ========================================================= */

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
}
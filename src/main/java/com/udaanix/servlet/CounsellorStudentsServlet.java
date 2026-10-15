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
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * COUNSELLOR STUDENTS SERVLET
 * ============================================================
 *
 * Endpoint:
 *
 * GET /api/counsellor/students
 *
 * Optional parameters:
 *
 * ?search=rahul
 * ?status=ACTIVE
 *
 * This servlet returns ONLY students assigned to the
 * currently logged-in counsellor.
 *
 * Database flow:
 *
 * student_counsellors
 *        ↓
 * students
 *        ↓
 * users
 *        ↓
 * student_career_interests
 *        ↓
 * careers
 *
 * ============================================================
 */
@WebServlet("/api/counsellor/students")
public class CounsellorStudentsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /* =========================================================
       GET STUDENTS
       ========================================================= */

    @Override
    protected void doGet(
            HttpServletRequest request,
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
           LOGIN CHECK
           ----------------------------------------------------- */

        Long counsellorId =
                getLoggedInUserId(session);

        if (counsellorId == null) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Counsellor is not logged in."
            );

            return;
        }


        /* -----------------------------------------------------
           ROLE CHECK
           ----------------------------------------------------- */

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


        /* =====================================================
           REQUEST FILTERS
           ===================================================== */

        String search =
                trim(request.getParameter("search"));

        String status =
                trim(request.getParameter("status"));


        /*
         * Default status:
         *
         * If no status is supplied, show ACTIVE students.
         *
         * If the frontend wants all students, it can send:
         *
         * status=ALL
         */

        if (status.isBlank()) {
            status = "ACTIVE";
        }


        /* =====================================================
           SQL
           ===================================================== */

        StringBuilder sql =
                new StringBuilder("""
                    SELECT
                        sc.assignment_id,
                        sc.status AS assignment_status,
                        sc.assigned_at,

                        s.student_id,
                        s.education_level,
                        s.qualification,
                        s.bio,
                        s.city,

                        u.first_name,
                        u.last_name,
                        u.email,
                        u.mobile,
                        u.profile_picture,
                        u.is_active,

                        ci.career_id,
                        ci.interest_level,
                        ci.source AS career_source,

                        c.career_name

                    FROM student_counsellors sc

                    INNER JOIN students s
                        ON sc.student_id = s.student_id

                    INNER JOIN users u
                        ON s.student_id = u.user_id

                    LEFT JOIN (
                        SELECT
                            sci.student_id,
                            sci.career_id,
                            sci.interest_level,
                            sci.source
                        FROM student_career_interests sci

                        INNER JOIN (
                            SELECT
                                student_id,
                                MAX(created_at) AS latest_created_at
                            FROM student_career_interests
                            GROUP BY student_id
                        ) latest
                            ON sci.student_id = latest.student_id
                            AND sci.created_at = latest.latest_created_at
                    ) ci
                        ON ci.student_id = s.student_id

                    LEFT JOIN careers c
                        ON ci.career_id = c.career_id

                    WHERE sc.counsellor_id = ?
                      AND u.role = 'STUDENT'
                      AND u.is_active = TRUE
                    """);


        List<Object> parameters =
                new ArrayList<>();


        parameters.add(counsellorId);


        /* =====================================================
           STATUS FILTER
           ===================================================== */

        if (!"ALL".equalsIgnoreCase(status)) {

            sql.append(
                    " AND sc.status = ? "
            );

            parameters.add(
                    status.toUpperCase()
            );
        }


        /* =====================================================
           SEARCH FILTER
           ===================================================== */

        if (!search.isBlank()) {

            sql.append("""
                     AND (
                            CONCAT(
                                COALESCE(u.first_name, ''),
                                ' ',
                                COALESCE(u.last_name, '')
                            ) LIKE ?
                            OR u.email LIKE ?
                            OR u.mobile LIKE ?
                            OR s.city LIKE ?
                            OR s.education_level LIKE ?
                            OR s.qualification LIKE ?
                            OR c.career_name LIKE ?
                         )
                    """);

            String searchValue =
                    "%" + search + "%";

            parameters.add(searchValue);
            parameters.add(searchValue);
            parameters.add(searchValue);
            parameters.add(searchValue);
            parameters.add(searchValue);
            parameters.add(searchValue);
            parameters.add(searchValue);
        }


        /* =====================================================
           ORDER
           ===================================================== */

        sql.append("""
                ORDER BY
                    sc.assigned_at DESC,
                    u.first_name ASC
                """);


        /* =====================================================
           DATABASE
           ===================================================== */

        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                sql.toString()
                        )
        ) {

            /* -------------------------------------------------
               SET PARAMETERS
               ------------------------------------------------- */

            for (int i = 0;
                 i < parameters.size();
                 i++) {

                ps.setObject(
                        i + 1,
                        parameters.get(i)
                );
            }


            /* -------------------------------------------------
               EXECUTE
               ------------------------------------------------- */

            try (ResultSet rs =
                         ps.executeQuery()) {


                /* =================================================
                   BUILD JSON
                   ================================================= */

                StringBuilder json =
                        new StringBuilder();

                json.append("{");

                json.append(
                        "\"success\":true,"
                );

                json.append(
                        "\"count\":"
                );


                /*
                 * We don't know count until rows
                 * are processed, so temporarily store
                 * student objects.
                 */

                List<String> studentJson =
                        new ArrayList<>();


                while (rs.next()) {

                    long assignmentId =
                            rs.getLong(
                                    "assignment_id"
                            );

                    long studentId =
                            rs.getLong(
                                    "student_id"
                            );

                    String assignmentStatus =
                            safe(
                                    rs.getString(
                                            "assignment_status"
                                    )
                            );

                    String assignedAt =
                            safe(
                                    rs.getString(
                                            "assigned_at"
                                    )
                            );

                    String firstName =
                            safe(
                                    rs.getString(
                                            "first_name"
                                    )
                            );

                    String lastName =
                            safe(
                                    rs.getString(
                                            "last_name"
                                    )
                            );

                    String email =
                            safe(
                                    rs.getString(
                                            "email"
                                    )
                            );

                    String mobile =
                            safe(
                                    rs.getString(
                                            "mobile"
                                    )
                            );

                    String profilePicture =
                            safe(
                                    rs.getString(
                                            "profile_picture"
                                    )
                            );

                    String educationLevel =
                            safe(
                                    rs.getString(
                                            "education_level"
                                    )
                            );

                    String qualification =
                            safe(
                                    rs.getString(
                                            "qualification"
                                    )
                            );

                    String bio =
                            safe(
                                    rs.getString(
                                            "bio"
                                    )
                            );

                    String city =
                            safe(
                                    rs.getString(
                                            "city"
                                    )
                            );

                    String careerId =
                            safe(
                                    rs.getString(
                                            "career_id"
                                    )
                            );

                    String careerName =
                            safe(
                                    rs.getString(
                                            "career_name"
                                    )
                            );

                    String interestLevel =
                            safe(
                                    rs.getString(
                                            "interest_level"
                                    )
                            );

                    String careerSource =
                            safe(
                                    rs.getString(
                                            "career_source"
                                    )
                            );

                    boolean isActive =
                            rs.getBoolean(
                                    "is_active"
                            );


                    /* =============================================
                       PROFILE PICTURE URL
                       ============================================= */

                    String profilePictureUrl = "";

                    if (!profilePicture.isBlank()) {

                        /*
                         * We intentionally return the filename
                         * here.
                         *
                         * Dashboard can build the image URL
                         * using the appropriate student profile
                         * picture endpoint.
                         */

                        profilePictureUrl =
                                profilePicture;
                    }


                    /* =============================================
                       STUDENT JSON
                       ============================================= */

                    StringBuilder student =
                            new StringBuilder();

                    student.append("{");


                    student.append(
                            "\"assignmentId\":"
                    ).append(
                            assignmentId
                    ).append(",");


                    student.append(
                            "\"studentId\":"
                    ).append(
                            studentId
                    ).append(",");


                    student.append(
                            "\"firstName\":\""
                    ).append(
                            escapeJson(firstName)
                    ).append("\",");


                    student.append(
                            "\"lastName\":\""
                    ).append(
                            escapeJson(lastName)
                    ).append("\",");


                    student.append(
                            "\"fullName\":\""
                    ).append(
                            escapeJson(
                                    buildFullName(
                                            firstName,
                                            lastName
                                    )
                            )
                    ).append("\",");


                    student.append(
                            "\"email\":\""
                    ).append(
                            escapeJson(email)
                    ).append("\",");


                    student.append(
                            "\"mobile\":\""
                    ).append(
                            escapeJson(mobile)
                    ).append("\",");


                    student.append(
                            "\"educationLevel\":\""
                    ).append(
                            escapeJson(
                                    educationLevel
                            )
                    ).append("\",");


                    student.append(
                            "\"qualification\":\""
                    ).append(
                            escapeJson(
                                    qualification
                            )
                    ).append("\",");


                    student.append(
                            "\"city\":\""
                    ).append(
                            escapeJson(city)
                    ).append("\",");


                    student.append(
                            "\"bio\":\""
                    ).append(
                            escapeJson(bio)
                    ).append("\",");


                    student.append(
                            "\"profilePicture\":\""
                    ).append(
                            escapeJson(
                                    profilePictureUrl
                            )
                    ).append("\",");


                    student.append(
                            "\"assignmentStatus\":\""
                    ).append(
                            escapeJson(
                                    assignmentStatus
                            )
                    ).append("\",");


                    student.append(
                            "\"assignedAt\":\""
                    ).append(
                            escapeJson(
                                    assignedAt
                            )
                    ).append("\",");


                    student.append(
                            "\"careerId\":\""
                    ).append(
                            escapeJson(careerId)
                    ).append("\",");


                    student.append(
                            "\"careerName\":\""
                    ).append(
                            escapeJson(careerName)
                    ).append("\",");


                    student.append(
                            "\"interestLevel\":\""
                    ).append(
                            escapeJson(interestLevel)
                    ).append("\",");


                    student.append(
                            "\"careerSource\":\""
                    ).append(
                            escapeJson(careerSource)
                    ).append("\",");


                    student.append(
                            "\"isActive\":"
                    ).append(
                            isActive
                    );


                    student.append("}");


                    studentJson.add(
                            student.toString()
                    );
                }


                /* =================================================
                   FINAL JSON
                   ================================================= */

                json.append(
                        studentJson.size()
                ).append(",");


                json.append(
                        "\"students\":["
                );


                for (int i = 0;
                     i < studentJson.size();
                     i++) {

                    if (i > 0) {
                        json.append(",");
                    }

                    json.append(
                            studentJson.get(i)
                    );
                }


                json.append("]");


                json.append("}");


                out.print(
                        json.toString()
                );
            }


        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load counsellor students."
            );
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


        Object userIdObject =
                session.getAttribute("userId");


        if (!(userIdObject instanceof Number)) {
            return null;
        }


        return ((Number) userIdObject).longValue();
    }


    /* =========================================================
       SAFE STRING
       ========================================================= */

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }


    /* =========================================================
       TRIM
       ========================================================= */

    private String trim(String value) {

        return value == null
                ? ""
                : value.trim();
    }


    /* =========================================================
       BUILD FULL NAME
       ========================================================= */

    private String buildFullName(
            String firstName,
            String lastName) {

        String first =
                safe(firstName).trim();

        String last =
                safe(lastName).trim();


        if (first.isEmpty()) {
            return last;
        }


        if (last.isEmpty()) {
            return first;
        }


        return first + " " + last;
    }


    /* =========================================================
       JSON ESCAPE
       ========================================================= */

    private String escapeJson(
            String value) {

        if (value == null) {
            return "";
        }


        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }


    /* =========================================================
       ERROR RESPONSE
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
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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * COUNSELLING SESSIONS SERVLET
 * ============================================================
 *
 * Endpoint:
 *
 * GET /api/counsellor/sessions
 *
 * Optional parameters:
 *
 * ?status=UPCOMING
 * ?status=PENDING
 * ?status=CONFIRMED
 * ?status=COMPLETED
 * ?status=CANCELLED
 * ?status=ALL
 *
 * Optional search:
 *
 * ?search=student
 *
 * This servlet returns sessions belonging ONLY to the
 * currently logged-in counsellor.
 *
 * Database:
 *
 * counselling_sessions
 *       ↓
 * students
 *       ↓
 * users
 *
 * ============================================================
 */

@WebServlet("/api/counsellor/sessions")
public class CounsellingSessionsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /* =========================================================
       GET
       ========================================================= */

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();


        /* =====================================================
           SESSION
           ===================================================== */

        HttpSession session =
                request.getSession(false);


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


        /* =====================================================
           ROLE CHECK
           ===================================================== */

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
           FILTERS
           ===================================================== */

        String status =
                trim(request.getParameter("status"));

        String search =
                trim(request.getParameter("search"));


        /*
         * Default:
         *
         * UPCOMING
         *
         * This is useful for the dashboard's
         * Upcoming Sessions section.
         */

        if (status.isBlank()) {
            status = "UPCOMING";
        }


        /* =====================================================
           SQL
           ===================================================== */

        StringBuilder sql =
                new StringBuilder("""
                    SELECT
                        cs.session_id,
                        cs.student_id,
                        cs.counsellor_id,
                        cs.scheduled_at,
                        cs.duration_minutes,
                        cs.session_type,
                        cs.status,
                        cs.meeting_link,
                        cs.notes,
                        cs.created_at,
                        cs.updated_at,

                        u.first_name,
                        u.last_name,
                        u.email,
                        u.mobile,
                        u.profile_picture,

                        s.education_level,
                        s.qualification,
                        s.city

                    FROM counselling_sessions cs

                    INNER JOIN students s
                        ON cs.student_id = s.student_id

                    INNER JOIN users u
                        ON s.student_id = u.user_id

                    WHERE cs.counsellor_id = ?
                      AND u.role = 'STUDENT'
                      AND u.is_active = TRUE
                    """);


        List<Object> parameters =
                new ArrayList<>();


        parameters.add(counsellorId);


        /* =====================================================
           STATUS FILTER
           ===================================================== */

        if ("UPCOMING".equalsIgnoreCase(status)) {

            sql.append("""
                     AND cs.scheduled_at >= NOW()
                     AND cs.status IN ('PENDING', 'CONFIRMED')
                    """);

        } else if ("PAST".equalsIgnoreCase(status)) {

            sql.append("""
                     AND cs.scheduled_at < NOW()
                    """);

        } else if (!"ALL".equalsIgnoreCase(status)) {

            /*
             * Exact database status:
             *
             * PENDING
             * CONFIRMED
             * COMPLETED
             * CANCELLED
             */

            sql.append(
                    " AND cs.status = ? "
            );

            parameters.add(
                    status.toUpperCase()
            );
        }


        /* =====================================================
           SEARCH
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
        }


        /* =====================================================
           ORDER
           ===================================================== */

        if ("PAST".equalsIgnoreCase(status) ||
                "COMPLETED".equalsIgnoreCase(status)) {

            sql.append(
                    " ORDER BY cs.scheduled_at DESC "
            );

        } else {

            sql.append(
                    " ORDER BY cs.scheduled_at ASC "
            );
        }


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
               PARAMETERS
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


                List<String> sessionJson =
                        new ArrayList<>();


                while (rs.next()) {

                    /* =========================================
                       SESSION DATA
                       ========================================= */

                    long sessionId =
                            rs.getLong(
                                    "session_id"
                            );


                    long studentId =
                            rs.getLong(
                                    "student_id"
                            );


                    long counsellorIdFromDb =
                            rs.getLong(
                                    "counsellor_id"
                            );


                    int durationMinutes =
                            rs.getInt(
                                    "duration_minutes"
                            );


                    String sessionType =
                            safe(
                                    rs.getString(
                                            "session_type"
                                    )
                            );


                    String sessionStatus =
                            safe(
                                    rs.getString(
                                            "status"
                                    )
                            );


                    String meetingLink =
                            safe(
                                    rs.getString(
                                            "meeting_link"
                                    )
                            );


                    String notes =
                            safe(
                                    rs.getString(
                                            "notes"
                                    )
                            );


                    /* =========================================
                       DATE / TIME
                       ========================================= */

                    Timestamp scheduledTimestamp =
                            rs.getTimestamp(
                                    "scheduled_at"
                            );


                    String scheduledAt = "";

                    if (scheduledTimestamp != null) {

                        scheduledAt =
                                DATE_TIME_FORMAT.format(
                                        scheduledTimestamp
                                );
                    }


                    Timestamp createdTimestamp =
                            rs.getTimestamp(
                                    "created_at"
                            );


                    String createdAt = "";

                    if (createdTimestamp != null) {

                        createdAt =
                                DATE_TIME_FORMAT.format(
                                        createdTimestamp
                                );
                    }


                    Timestamp updatedTimestamp =
                            rs.getTimestamp(
                                    "updated_at"
                            );


                    String updatedAt = "";

                    if (updatedTimestamp != null) {

                        updatedAt =
                                DATE_TIME_FORMAT.format(
                                        updatedTimestamp
                                );
                    }


                    /* =========================================
                       STUDENT
                       ========================================= */

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


                    String fullName =
                            buildFullName(
                                    firstName,
                                    lastName
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


                    String city =
                            safe(
                                    rs.getString(
                                            "city"
                                    )
                            );


                    /* =========================================
                       JSON
                       ========================================= */

                    StringBuilder item =
                            new StringBuilder();


                    item.append("{");


                    item.append(
                            "\"sessionId\":"
                    ).append(
                            sessionId
                    ).append(",");


                    item.append(
                            "\"studentId\":"
                    ).append(
                            studentId
                    ).append(",");


                    item.append(
                            "\"counsellorId\":"
                    ).append(
                            counsellorIdFromDb
                    ).append(",");


                    item.append(
                            "\"scheduledAt\":\""
                    ).append(
                            escapeJson(
                                    scheduledAt
                            )
                    ).append("\",");


                    item.append(
                            "\"durationMinutes\":"
                    ).append(
                            durationMinutes
                    ).append(",");


                    item.append(
                            "\"sessionType\":\""
                    ).append(
                            escapeJson(
                                    sessionType
                            )
                    ).append("\",");


                    item.append(
                            "\"status\":\""
                    ).append(
                            escapeJson(
                                    sessionStatus
                            )
                    ).append("\",");


                    item.append(
                            "\"meetingLink\":\""
                    ).append(
                            escapeJson(
                                    meetingLink
                            )
                    ).append("\",");


                    item.append(
                            "\"notes\":\""
                    ).append(
                            escapeJson(
                                    notes
                            )
                    ).append(",");


                    item.append(
                            "\"createdAt\":\""
                    ).append(
                            escapeJson(
                                    createdAt
                            )
                    ).append("\",");


                    item.append(
                            "\"updatedAt\":\""
                    ).append(
                            escapeJson(
                                    updatedAt
                            )
                    ).append(",");


                    /* =========================================
                       STUDENT DETAILS
                       ========================================= */

                    item.append(
                            "\"student\":{"
                    );


                    item.append(
                            "\"studentId\":"
                    ).append(
                            studentId
                    ).append(",");


                    item.append(
                            "\"firstName\":\""
                    ).append(
                            escapeJson(
                                    firstName
                            )
                    ).append("\",");


                    item.append(
                            "\"lastName\":\""
                    ).append(
                            escapeJson(
                                    lastName
                            )
                    ).append(",");


                    item.append(
                            "\"fullName\":\""
                    ).append(
                            escapeJson(
                                    fullName
                            )
                    ).append("\",");


                    item.append(
                            "\"email\":\""
                    ).append(
                            escapeJson(
                                    email
                            )
                    ).append("\",");


                    item.append(
                            "\"mobile\":\""
                    ).append(
                            escapeJson(
                                    mobile
                            )
                    ).append("\",");


                    item.append(
                            "\"profilePicture\":\""
                    ).append(
                            escapeJson(
                                    profilePicture
                            )
                    ).append("\",");


                    item.append(
                            "\"educationLevel\":\""
                    ).append(
                            escapeJson(
                                    educationLevel
                            )
                    ).append("\",");


                    item.append(
                            "\"qualification\":\""
                    ).append(
                            escapeJson(
                                    qualification
                            )
                    ).append("\",");


                    item.append(
                            "\"city\":\""
                    ).append(
                            escapeJson(
                                    city
                            )
                    ).append("\"");


                    item.append("}");


                    item.append("}");


                    sessionJson.add(
                            item.toString()
                    );
                }


                /* =================================================
                   FINAL RESPONSE
                   ================================================= */

                StringBuilder json =
                        new StringBuilder();


                json.append("{");


                json.append(
                        "\"success\":true,"
                );


                json.append(
                        "\"filter\":\""
                ).append(
                        escapeJson(
                                status.toUpperCase()
                        )
                ).append("\",");


                json.append(
                        "\"count\":"
                ).append(
                        sessionJson.size()
                ).append(",");


                json.append(
                        "\"sessions\":["
                );


                for (int i = 0;
                     i < sessionJson.size();
                     i++) {

                    if (i > 0) {
                        json.append(",");
                    }

                    json.append(
                            sessionJson.get(i)
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
                    "Unable to load counselling sessions."
            );
        }
    }


    /* =========================================================
       SESSION USER ID
       ========================================================= */

    private Long getLoggedInUserId(
            HttpSession session) {

        if (session == null) {
            return null;
        }


        Object userIdObject =
                session.getAttribute("userId");


        /*
         * Important:
         *
         * Use Number instead of Integer because
         * AuthLoginServlet may store Long.
         */

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
       FULL NAME
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
       ERROR
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
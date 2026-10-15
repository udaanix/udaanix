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
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/counsellor/messages")
public class CounsellorMessagesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;


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
           LOGIN CHECK
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
                        ? safe(
                            (String)
                            session.getAttribute("role")
                          )
                        : "";

        if (!"COUNSELLOR".equalsIgnoreCase(role)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Access denied. Counsellor account required."
            );

            return;
        }


        /* =====================================================
           PARAMETERS
           ===================================================== */

        String studentIdParam =
                trim(
                    request.getParameter("studentId")
                );

        String unreadParam =
                trim(
                    request.getParameter("unread")
                );

        String search =
                trim(
                    request.getParameter("search")
                );


        Long studentId = null;


        if (!studentIdParam.isBlank()) {

            try {

                studentId =
                        Long.parseLong(
                                studentIdParam
                        );

            } catch (NumberFormatException e) {

                sendError(
                        response,
                        out,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid studentId."
                );

                return;
            }
        }


        boolean unreadOnly =
                "true".equalsIgnoreCase(
                        unreadParam
                );


        /* =====================================================
           SQL
           ===================================================== */

        StringBuilder sql =
                new StringBuilder("""
                    SELECT
                        m.message_id,
                        m.sender_id,
                        m.receiver_id,
                        m.booking_id,
                        m.message_text,
                        m.sent_at,
                        m.is_read,

                        u.user_id AS student_user_id,
                        u.first_name,
                        u.last_name,
                        u.email,
                        u.mobile,
                        u.profile_picture,

                        s.education_level,
                        s.qualification,
                        s.city

                    FROM messages m

                    INNER JOIN users u
                        ON (
                            CASE
                                WHEN m.sender_id = ?
                                THEN m.receiver_id
                                ELSE m.sender_id
                            END
                        ) = u.user_id

                    LEFT JOIN students s
                        ON s.student_id = u.user_id

                    WHERE
                        (
                            m.sender_id = ?
                            OR
                            m.receiver_id = ?
                        )

                    AND u.role = 'STUDENT'
                    AND u.is_active = TRUE
                    """);


        List<Object> parameters =
                new ArrayList<>();


        /*
         * First parameter:
         *
         * CASE WHEN sender_id = counsellor
         * THEN receiver
         * ELSE sender
         */

        parameters.add(counsellorId);

        parameters.add(counsellorId);

        parameters.add(counsellorId);


        /* =====================================================
           SPECIFIC STUDENT
           ===================================================== */

        if (studentId != null) {

            sql.append("""
                    AND (
                        (
                            m.sender_id = ?
                            AND
                            m.receiver_id = ?
                        )
                        OR
                        (
                            m.sender_id = ?
                            AND
                            m.receiver_id = ?
                        )
                    )
                    """);

            parameters.add(counsellorId);
            parameters.add(studentId);

            parameters.add(studentId);
            parameters.add(counsellorId);
        }


        /* =====================================================
           UNREAD ONLY
           ===================================================== */

        if (unreadOnly) {

            sql.append("""
                    AND m.receiver_id = ?
                    AND m.is_read = 0
                    """);

            parameters.add(counsellorId);
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

                        OR m.message_text LIKE ?
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

        sql.append(
                " ORDER BY m.sent_at DESC "
        );


        /* =====================================================
           EXECUTE
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
               RESULT
               ------------------------------------------------- */

            try (ResultSet rs =
                         ps.executeQuery()) {


                List<String> messages =
                        new ArrayList<>();


                int unreadCount = 0;


                while (rs.next()) {


                    /* =========================================
                       MESSAGE
                       ========================================= */

                    long messageId =
                            rs.getLong(
                                    "message_id"
                            );


                    long senderId =
                            rs.getLong(
                                    "sender_id"
                            );


                    long receiverId =
                            rs.getLong(
                                    "receiver_id"
                            );


                    int bookingId =
                            rs.getInt(
                                    "booking_id"
                            );


                    String messageText =
                            safe(
                                rs.getString(
                                    "message_text"
                                )
                            );


                    boolean isRead =
                            rs.getBoolean(
                                    "is_read"
                            );


                    if (!isRead &&
                        receiverId == counsellorId) {

                        unreadCount++;
                    }


                    Timestamp sentTimestamp =
                            rs.getTimestamp(
                                    "sent_at"
                            );


                    String sentAt = "";


                    if (sentTimestamp != null) {

                        sentAt =
                                sentTimestamp.toString();
                    }


                    /* =========================================
                       STUDENT
                       ========================================= */

                    long studentUserId =
                            rs.getLong(
                                    "student_user_id"
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
                        "\"messageId\":"
                    ).append(
                        messageId
                    ).append(",");


                    item.append(
                        "\"senderId\":"
                    ).append(
                        senderId
                    ).append(",");


                    item.append(
                        "\"receiverId\":"
                    ).append(
                        receiverId
                    ).append(",");


                    item.append(
                        "\"bookingId\":"
                    ).append(
                        bookingId
                    ).append(",");


                    item.append(
                        "\"messageText\":\""
                    ).append(
                        escapeJson(
                            messageText
                        )
                    ).append("\",");


                    item.append(
                        "\"sentAt\":\""
                    ).append(
                        escapeJson(
                            sentAt
                        )
                    ).append("\",");


                    item.append(
                        "\"isRead\":"
                    ).append(
                        isRead
                    ).append(",");


                    /* =========================================
                       STUDENT OBJECT
                       ========================================= */

                    item.append(
                        "\"student\":{"
                    );


                    item.append(
                        "\"studentId\":"
                    ).append(
                        studentUserId
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
                    ).append(",");


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
                    ).append(",");


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
                    ).append(",");


                    item.append(
                        "\"qualification\":\""
                    ).append(
                        escapeJson(
                            qualification
                        )
                    ).append(",");


                    item.append(
                        "\"city\":\""
                    ).append(
                        escapeJson(
                            city
                        )
                    ).append("\"");


                    item.append("}");


                    item.append("}");


                    messages.add(
                        item.toString()
                    );
                }


                /* =================================================
                   FINAL JSON
                   ================================================= */

                StringBuilder json =
                        new StringBuilder();


                json.append("{");


                json.append(
                    "\"success\":true,"
                );


                json.append(
                    "\"count\":"
                ).append(
                    messages.size()
                ).append(",");


                json.append(
                    "\"unreadCount\":"
                ).append(
                    unreadCount
                ).append(",");


                json.append(
                    "\"messages\":["
                );


                for (int i = 0;
                     i < messages.size();
                     i++) {

                    if (i > 0) {
                        json.append(",");
                    }

                    json.append(
                        messages.get(i)
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
                    "Unable to load counsellor messages."
            );
        }
    }


    /* =========================================================
       POST — SEND MESSAGE
       ========================================================= */

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out =
                response.getWriter();


        /* =====================================================
           LOGIN
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


        String role =
                session != null
                        ? safe(
                            (String)
                            session.getAttribute("role")
                          )
                        : "";


        if (!"COUNSELLOR".equalsIgnoreCase(role)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Access denied."
            );

            return;
        }


        /* =====================================================
           PARAMETERS
           ===================================================== */

        String receiverIdParam =
                trim(
                    request.getParameter(
                        "receiverId"
                    )
                );


        String messageText =
                trim(
                    request.getParameter(
                        "messageText"
                    )
                );


        String bookingIdParam =
                trim(
                    request.getParameter(
                        "bookingId"
                    )
                );


        if (receiverIdParam.isBlank()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "receiverId is required."
            );

            return;
        }


        if (messageText.isBlank()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Message cannot be empty."
            );

            return;
        }


        if (messageText.length() > 5000) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Message is too long. Maximum 5000 characters."
            );

            return;
        }


        long receiverId;


        try {

            receiverId =
                    Long.parseLong(
                        receiverIdParam
                    );

        } catch (NumberFormatException e) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid receiverId."
            );

            return;
        }


        Integer bookingId = null;


        if (!bookingIdParam.isBlank()) {

            try {

                bookingId =
                        Integer.parseInt(
                            bookingIdParam
                        );

            } catch (NumberFormatException e) {

                sendError(
                        response,
                        out,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid bookingId."
                );

                return;
            }
        }


        /* =====================================================
           DATABASE
           ===================================================== */

        try (
                Connection con =
                        DBConnection.getConnection()
        ) {


            /* -------------------------------------------------
               CHECK RECEIVER
               ------------------------------------------------- */

            String checkStudentSql = """
                SELECT user_id
                FROM users
                WHERE user_id = ?
                  AND role = 'STUDENT'
                  AND is_active = TRUE
                """;


            try (
                PreparedStatement checkPs =
                    con.prepareStatement(
                        checkStudentSql
                    )
            ) {

                checkPs.setLong(
                    1,
                    receiverId
                );


                try (
                    ResultSet rs =
                        checkPs.executeQuery()
                ) {

                    if (!rs.next()) {

                        sendError(
                                response,
                                out,
                                HttpServletResponse.SC_BAD_REQUEST,
                                "Student account not found."
                        );

                        return;
                    }
                }
            }


            /* -------------------------------------------------
               INSERT MESSAGE
               ------------------------------------------------- */

            String insertSql;


            if (bookingId != null) {

                insertSql = """
                    INSERT INTO messages
                    (
                        sender_id,
                        receiver_id,
                        booking_id,
                        message_text,
                        is_read
                    )
                    VALUES (?, ?, ?, ?, 0)
                    """;

            } else {

                insertSql = """
                    INSERT INTO messages
                    (
                        sender_id,
                        receiver_id,
                        message_text,
                        is_read
                    )
                    VALUES (?, ?, ?, 0)
                    """;
            }


            long newMessageId;


            try (
                PreparedStatement ps =
                    con.prepareStatement(
                        insertSql,
                        java.sql.Statement.RETURN_GENERATED_KEYS
                    )
            ) {


                ps.setLong(
                    1,
                    counsellorId
                );


                ps.setLong(
                    2,
                    receiverId
                );


                if (bookingId != null) {

                    ps.setInt(
                        3,
                        bookingId
                    );

                    ps.setString(
                        4,
                        messageText
                    );

                } else {

                    ps.setString(
                        3,
                        messageText
                    );
                }


                ps.executeUpdate();


                try (
                    ResultSet keys =
                        ps.getGeneratedKeys()
                ) {

                    if (keys.next()) {

                        newMessageId =
                                keys.getLong(1);

                    } else {

                        newMessageId = -1;
                    }
                }
            }


            /* -------------------------------------------------
               RESPONSE
               ------------------------------------------------- */

            out.print(
                "{"
                + "\"success\":true,"
                + "\"message\":\"Message sent successfully.\","
                + "\"messageId\":"
                + newMessageId
                + "}"
            );


        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to send message."
            );
        }
    }


    /* =========================================================
       PUT — MARK AS READ
       ========================================================= */

    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out =
                response.getWriter();


        /* =====================================================
           LOGIN
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


        String role =
                session != null
                        ? safe(
                            (String)
                            session.getAttribute("role")
                          )
                        : "";


        if (!"COUNSELLOR".equalsIgnoreCase(role)) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_FORBIDDEN,
                    "Access denied."
            );

            return;
        }


        /* =====================================================
           PARAMETERS
           ===================================================== */

        String messageIdParam =
                trim(
                    request.getParameter(
                        "messageId"
                    )
                );


        String studentIdParam =
                trim(
                    request.getParameter(
                        "studentId"
                    )
                );


        if (messageIdParam.isBlank() &&
            studentIdParam.isBlank()) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "messageId or studentId is required."
            );

            return;
        }


        try (
            Connection con =
                DBConnection.getConnection()
        ) {


            int updatedRows;


            /* =================================================
               MARK SINGLE MESSAGE
               ================================================= */

            if (!messageIdParam.isBlank()) {

                long messageId =
                        Long.parseLong(
                            messageIdParam
                        );


                String sql = """
                    UPDATE messages
                    SET is_read = 1
                    WHERE message_id = ?
                      AND receiver_id = ?
                    """;


                try (
                    PreparedStatement ps =
                        con.prepareStatement(sql)
                ) {

                    ps.setLong(
                        1,
                        messageId
                    );

                    ps.setLong(
                        2,
                        counsellorId
                    );


                    updatedRows =
                        ps.executeUpdate();
                }


            } else {

                /* =============================================
                   MARK STUDENT CONVERSATION READ
                   ============================================= */

                long studentId =
                        Long.parseLong(
                            studentIdParam
                        );


                String sql = """
                    UPDATE messages
                    SET is_read = 1
                    WHERE sender_id = ?
                      AND receiver_id = ?
                      AND is_read = 0
                    """;


                try (
                    PreparedStatement ps =
                        con.prepareStatement(sql)
                ) {

                    ps.setLong(
                        1,
                        studentId
                    );

                    ps.setLong(
                        2,
                        counsellorId
                    );


                    updatedRows =
                        ps.executeUpdate();
                }
            }


            out.print(
                "{"
                + "\"success\":true,"
                + "\"message\":\"Messages marked as read.\","
                + "\"updated\":"
                + updatedRows
                + "}"
            );


        } catch (NumberFormatException e) {

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid ID."
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendError(
                    response,
                    out,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to update message status."
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
                session.getAttribute(
                    "userId"
                );


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
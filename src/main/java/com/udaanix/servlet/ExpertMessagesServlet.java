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

@WebServlet("/api/expert/messages")
public class ExpertMessagesServlet extends HttpServlet {

    /*
     * ============================================================
     * GET MESSAGES
     * ============================================================
     *
     * GET:
     *
     * /api/expert/messages
     *
     * Returns all messages related to logged-in expert.
     *
     * Optional:
     *
     * /api/expert/messages?studentId=5
     *
     * Returns conversation between expert and one student.
     */

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            // --------------------------------------------------------
            // CHECK LOGIN
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
            // OPTIONAL STUDENT ID
            // --------------------------------------------------------

            String studentIdParam =
                    request.getParameter("studentId");

            if (studentIdParam != null
                    && !studentIdParam.isBlank()) {

                int studentId;

                try {

                    studentId =
                            Integer.parseInt(
                                    studentIdParam.trim()
                            );

                } catch (NumberFormatException e) {

                    response.setStatus(
                            HttpServletResponse.SC_BAD_REQUEST
                    );

                    out.println("""
                        {
                            "success": false,
                            "message": "Invalid student ID."
                        }
                        """);

                    return;
                }

                getConversation(
                        expertId,
                        studentId,
                        out,
                        response
                );

                return;
            }

            // --------------------------------------------------------
            // GET ALL MESSAGES
            // --------------------------------------------------------

            getAllMessages(
                    expertId,
                    out,
                    response
            );
        }
    }


    /*
     * ============================================================
     * GET ALL MESSAGES
     * ============================================================
     */

    private void getAllMessages(
            int expertId,
            PrintWriter out,
            HttpServletResponse response) {

        /*
         * NOTE:
         *
         * This query intentionally uses only the common
         * message-related fields expected in the Udaanix
         * messages table.
         */

        String sql = """
            SELECT
                m.message_id,
                m.sender_id,
                m.receiver_id,
                m.message_text,
                m.is_read,
                m.created_at,

                u.first_name,
                u.last_name,
                u.email,
                u.profile_picture

            FROM messages m

            INNER JOIN users u
                ON u.user_id =
                   CASE
                       WHEN m.sender_id = ?
                       THEN m.receiver_id
                       ELSE m.sender_id
                   END

            WHERE m.sender_id = ?
               OR m.receiver_id = ?

            ORDER BY m.created_at DESC
            """;

        StringBuilder messagesJson =
                new StringBuilder();

        messagesJson.append("[");

        boolean first = true;
        int unreadCount = 0;
        int totalMessages = 0;

        try (Connection connection =
                     DBConnection.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setInt(1, expertId);
            ps.setInt(2, expertId);
            ps.setInt(3, expertId);

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    if (!first) {
                        messagesJson.append(",");
                    }

                    first = false;
                    totalMessages++;

                    int messageId =
                            rs.getInt("message_id");

                    int senderId =
                            rs.getInt("sender_id");

                    int receiverId =
                            rs.getInt("receiver_id");

                    String messageText =
                            safe(
                                rs.getString(
                                    "message_text"
                                )
                            );

                    boolean isRead =
                            rs.getBoolean("is_read");

                    if (!isRead
                            && receiverId == expertId) {

                        unreadCount++;
                    }

                    String otherUserFirstName =
                            safe(
                                rs.getString(
                                    "first_name"
                                )
                            );

                    String otherUserLastName =
                            safe(
                                rs.getString(
                                    "last_name"
                                )
                            );

                    String otherUserName =
                            (
                                otherUserFirstName
                                + " "
                                + otherUserLastName
                            ).trim();

                    String otherUserEmail =
                            safe(
                                rs.getString(
                                    "email"
                                )
                            );

                    String otherUserPicture =
                            safe(
                                rs.getString(
                                    "profile_picture"
                                )
                            );

                    String createdAt =
                            safe(
                                rs.getString(
                                    "created_at"
                                )
                            );

                    messagesJson.append("{");

                    messagesJson.append(
                            "\"messageId\":"
                    ).append(
                            messageId
                    ).append(",");

                    messagesJson.append(
                            "\"senderId\":"
                    ).append(
                            senderId
                    ).append(",");

                    messagesJson.append(
                            "\"receiverId\":"
                    ).append(
                            receiverId
                    ).append(",");

                    messagesJson.append(
                            "\"message\":\""
                    ).append(
                            json(messageText)
                    ).append("\",");

                    messagesJson.append(
                            "\"isRead\":"
                    ).append(
                            isRead
                    ).append(",");

                    messagesJson.append(
                            "\"createdAt\":\""
                    ).append(
                            json(createdAt)
                    ).append("\",");

                    messagesJson.append(
                            "\"otherUser\":{"
                    );

                    messagesJson.append(
                            "\"id\":"
                    ).append(
                            senderId == expertId
                                    ? receiverId
                                    : senderId
                    ).append(",");

                    messagesJson.append(
                            "\"name\":\""
                    ).append(
                            json(otherUserName)
                    ).append("\",");

                    messagesJson.append(
                            "\"email\":\""
                    ).append(
                            json(otherUserEmail)
                    ).append("\",");

                    messagesJson.append(
                            "\"profilePicture\":\""
                    ).append(
                            json(otherUserPicture)
                    ).append("\"");

                    messagesJson.append("}");

                    messagesJson.append("}");
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
                    "message": "Unable to load messages."
                }
                """);

            return;
        }

        messagesJson.append("]");

        // ------------------------------------------------------------
        // RESPONSE
        // ------------------------------------------------------------

        response.setStatus(
                HttpServletResponse.SC_OK
        );

        out.println("{");

        out.println(
                "\"success\":true,"
        );

        out.println(
                "\"totalMessages\":"
                + totalMessages
                + ","
        );

        out.println(
                "\"unreadCount\":"
                + unreadCount
                + ","
        );

        out.println(
                "\"messages\":"
                + messagesJson
        );

        out.println("}");
    }


    /*
     * ============================================================
     * GET ONE CONVERSATION
     * ============================================================
     *
     * GET:
     *
     * /api/expert/messages?studentId=5
     */

    private void getConversation(
            int expertId,
            int studentId,
            PrintWriter out,
            HttpServletResponse response) {

        String sql = """
            SELECT
                m.message_id,
                m.sender_id,
                m.receiver_id,
                m.message_text,
                m.is_read,
                m.created_at,

                u.first_name,
                u.last_name,
                u.email,
                u.profile_picture

            FROM messages m

            INNER JOIN users u
                ON u.user_id = m.sender_id

            WHERE
                (
                    m.sender_id = ?
                    AND m.receiver_id = ?
                )
                OR
                (
                    m.sender_id = ?
                    AND m.receiver_id = ?
                )

            ORDER BY m.created_at ASC
            """;

        StringBuilder messagesJson =
                new StringBuilder();

        messagesJson.append("[");

        boolean first = true;

        try (Connection connection =
                     DBConnection.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setInt(1, expertId);
            ps.setInt(2, studentId);
            ps.setInt(3, studentId);
            ps.setInt(4, expertId);

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    if (!first) {
                        messagesJson.append(",");
                    }

                    first = false;

                    int messageId =
                            rs.getInt("message_id");

                    int senderId =
                            rs.getInt("sender_id");

                    int receiverId =
                            rs.getInt("receiver_id");

                    String messageText =
                            safe(
                                rs.getString(
                                    "message_text"
                                )
                            );

                    boolean isRead =
                            rs.getBoolean("is_read");

                    String createdAt =
                            safe(
                                rs.getString(
                                    "created_at"
                                )
                            );

                    String senderFirstName =
                            safe(
                                rs.getString(
                                    "first_name"
                                )
                            );

                    String senderLastName =
                            safe(
                                rs.getString(
                                    "last_name"
                                )
                            );

                    String senderName =
                            (
                                senderFirstName
                                + " "
                                + senderLastName
                            ).trim();

                    String senderEmail =
                            safe(
                                rs.getString(
                                    "email"
                                )
                            );

                    String senderPicture =
                            safe(
                                rs.getString(
                                    "profile_picture"
                                )
                            );

                    messagesJson.append("{");

                    messagesJson.append(
                            "\"messageId\":"
                    ).append(
                            messageId
                    ).append(",");

                    messagesJson.append(
                            "\"senderId\":"
                    ).append(
                            senderId
                    ).append(",");

                    messagesJson.append(
                            "\"receiverId\":"
                    ).append(
                            receiverId
                    ).append(",");

                    messagesJson.append(
                            "\"message\":\""
                    ).append(
                            json(messageText)
                    ).append("\",");

                    messagesJson.append(
                            "\"isMine\":"
                    ).append(
                            senderId == expertId
                    ).append(",");

                    messagesJson.append(
                            "\"isRead\":"
                    ).append(
                            isRead
                    ).append(",");

                    messagesJson.append(
                            "\"createdAt\":\""
                    ).append(
                            json(createdAt)
                    ).append("\",");

                    messagesJson.append(
                            "\"sender\":{"
                    );

                    messagesJson.append(
                            "\"id\":"
                    ).append(
                            senderId
                    ).append(",");

                    messagesJson.append(
                            "\"name\":\""
                    ).append(
                            json(senderName)
                    ).append("\",");

                    messagesJson.append(
                            "\"email\":\""
                    ).append(
                            json(senderEmail)
                    ).append("\",");

                    messagesJson.append(
                            "\"profilePicture\":\""
                    ).append(
                            json(senderPicture)
                    ).append("\"");

                    messagesJson.append("}");

                    messagesJson.append("}");
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
                    "message": "Unable to load conversation."
                }
                """);

            return;
        }

        messagesJson.append("]");

        // ------------------------------------------------------------
        // RESPONSE
        // ------------------------------------------------------------

        response.setStatus(
                HttpServletResponse.SC_OK
        );

        out.println("{");

        out.println(
                "\"success\":true,"
        );

        out.println(
                "\"expertId\":"
                + expertId
                + ","
        );

        out.println(
                "\"studentId\":"
                + studentId
                + ","
        );

        out.println(
                "\"messages\":"
                + messagesJson
        );

        out.println("}");
    }


    /*
     * ============================================================
     * SEND MESSAGE
     * ============================================================
     *
     * POST:
     *
     * /api/expert/messages
     *
     * Parameters:
     *
     * studentId
     * message
     */

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            // --------------------------------------------------------
            // CHECK LOGIN
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

            String studentIdParam =
                    request.getParameter("studentId");

            String message =
                    request.getParameter("message");

            if (studentIdParam == null
                    || studentIdParam.isBlank()
                    || message == null
                    || message.isBlank()) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Student ID and message are required."
                    }
                    """);

                return;
            }

            int studentId;

            try {

                studentId =
                        Integer.parseInt(
                                studentIdParam.trim()
                        );

            } catch (NumberFormatException e) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Invalid student ID."
                    }
                    """);

                return;
            }

            message =
                    message.trim();

            if (message.length() > 5000) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Message cannot exceed 5000 characters."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // VERIFY STUDENT
            // --------------------------------------------------------

            String verifyStudentSQL = """
                SELECT user_id
                FROM users
                WHERE user_id = ?
                  AND role = 'STUDENT'
                  AND is_active = 1
                """;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(
                                 verifyStudentSQL
                         )) {

                ps.setInt(1, studentId);

                try (ResultSet rs =
                             ps.executeQuery()) {

                    if (!rs.next()) {

                        response.setStatus(
                                HttpServletResponse.SC_NOT_FOUND
                        );

                        out.println("""
                            {
                                "success": false,
                                "message": "Student not found."
                            }
                            """);

                        return;
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
                        "message": "Unable to verify student."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // VERIFY EXPERT-STUDENT RELATIONSHIP
            // --------------------------------------------------------

            String relationSQL = """
                SELECT booking_id
                FROM expert_bookings
                WHERE expert_id = ?
                  AND student_id = ?
                LIMIT 1
                """;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(
                                 relationSQL
                         )) {

                ps.setInt(1, expertId);
                ps.setInt(2, studentId);

                try (ResultSet rs =
                             ps.executeQuery()) {

                    if (!rs.next()) {

                        response.setStatus(
                                HttpServletResponse.SC_FORBIDDEN
                        );

                        out.println("""
                            {
                                "success": false,
                                "message": "You can only message students connected with your expert account."
                            }
                            """);

                        return;
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
                        "message": "Unable to verify messaging access."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // INSERT MESSAGE
            // --------------------------------------------------------

            String insertSQL = """
                INSERT INTO messages
                (
                    sender_id,
                    receiver_id,
                    message_text,
                    is_read
                )
                VALUES (?, ?, ?, FALSE)
                """;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(
                                 insertSQL
                         )) {

                ps.setInt(1, expertId);
                ps.setInt(2, studentId);
                ps.setString(3, message);

                int inserted =
                        ps.executeUpdate();

                if (inserted == 0) {

                    response.setStatus(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                    );

                    out.println("""
                        {
                            "success": false,
                            "message": "Message could not be sent."
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
                        "message": "Unable to send message."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // SUCCESS
            // --------------------------------------------------------

            response.setStatus(
                    HttpServletResponse.SC_CREATED
            );

            out.println("""
                {
                    "success": true,
                    "message": "Message sent successfully."
                }
                """);
        }
    }


    /*
     * ============================================================
     * MARK MESSAGE(S) AS READ
     * ============================================================
     *
     * PUT:
     *
     * /api/expert/messages
     *
     * Parameters:
     *
     * messageId
     */

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            // --------------------------------------------------------
            // CHECK LOGIN
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

            String messageIdParam =
                    request.getParameter("messageId");

            if (messageIdParam == null
                    || messageIdParam.isBlank()) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Message ID is required."
                    }
                    """);

                return;
            }

            int messageId;

            try {

                messageId =
                        Integer.parseInt(
                                messageIdParam.trim()
                        );

            } catch (NumberFormatException e) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Invalid message ID."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // MARK AS READ
            // --------------------------------------------------------

            String updateSQL = """
                UPDATE messages
                SET is_read = TRUE
                WHERE message_id = ?
                  AND receiver_id = ?
                """;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(
                                 updateSQL
                         )) {

                ps.setInt(1, messageId);
                ps.setInt(2, expertId);

                int updated =
                        ps.executeUpdate();

                if (updated == 0) {

                    response.setStatus(
                            HttpServletResponse.SC_NOT_FOUND
                    );

                    out.println("""
                        {
                            "success": false,
                            "message": "Message not found."
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
                        "message": "Unable to mark message as read."
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
                    "message": "Message marked as read."
                }
                """);
        }
    }


    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

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
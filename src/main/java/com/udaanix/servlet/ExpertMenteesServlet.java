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

@WebServlet("/api/expert/mentees")
public class ExpertMenteesServlet extends HttpServlet {

    /*
     * ============================================================
     * GET MY MENTEES
     * ============================================================
     *
     * GET:
     * /api/expert/mentees
     *
     * Returns unique students who have bookings
     * with the logged-in expert.
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
            // GET UNIQUE MENTEES
            // --------------------------------------------------------

            String sql = """
                SELECT
                    u.user_id,
                    u.first_name,
                    u.last_name,
                    u.email,
                    u.mobile,
                    u.profile_picture,

                    COUNT(eb.booking_id) AS total_sessions,

                    SUM(
                        CASE
                            WHEN eb.status = 'COMPLETED'
                            THEN 1
                            ELSE 0
                        END
                    ) AS completed_sessions,

                    MAX(eb.scheduled_at) AS last_session,

                    MIN(
                        CASE
                            WHEN eb.scheduled_at >= NOW()
                             AND eb.status IN ('PENDING', 'CONFIRMED')
                            THEN eb.scheduled_at
                            ELSE NULL
                        END
                    ) AS next_session

                FROM expert_bookings eb

                INNER JOIN users u
                    ON eb.student_id = u.user_id

                WHERE eb.expert_id = ?
                  AND u.role = 'STUDENT'

                GROUP BY
                    u.user_id,
                    u.first_name,
                    u.last_name,
                    u.email,
                    u.mobile,
                    u.profile_picture

                ORDER BY
                    next_session IS NULL,
                    next_session ASC,
                    last_session DESC
                """;

            StringBuilder menteesJson =
                    new StringBuilder();

            menteesJson.append("[");

            boolean first = true;
            int totalMentees = 0;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(sql)) {

                ps.setInt(1, expertId);

                try (ResultSet rs =
                             ps.executeQuery()) {

                    while (rs.next()) {

                        if (!first) {
                            menteesJson.append(",");
                        }

                        first = false;
                        totalMentees++;

                        // ------------------------------------------------
                        // STUDENT BASIC DATA
                        // ------------------------------------------------

                        int studentId =
                                rs.getInt("user_id");

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
                                (firstName + " " + lastName)
                                .trim();

                        String email =
                                safe(
                                    rs.getString("email")
                                );

                        String mobile =
                                safe(
                                    rs.getString("mobile")
                                );

                        String profilePicture =
                                safe(
                                    rs.getString(
                                        "profile_picture"
                                    )
                                );

                        // ------------------------------------------------
                        // SESSION DATA
                        // ------------------------------------------------

                        int totalSessions =
                                rs.getInt("total_sessions");

                        int completedSessions =
                                rs.getInt(
                                    "completed_sessions"
                                );

                        String lastSession =
                                safe(
                                    rs.getString(
                                        "last_session"
                                    )
                                );

                        String nextSession =
                                safe(
                                    rs.getString(
                                        "next_session"
                                    )
                                );

                        // ------------------------------------------------
                        // JSON
                        // ------------------------------------------------

                        menteesJson.append("{");

                        menteesJson.append(
                                "\"studentId\":"
                        ).append(
                                studentId
                        ).append(",");

                        menteesJson.append(
                                "\"firstName\":\""
                        ).append(
                                json(firstName)
                        ).append("\",");

                        menteesJson.append(
                                "\"lastName\":\""
                        ).append(
                                json(lastName)
                        ).append("\",");

                        menteesJson.append(
                                "\"fullName\":\""
                        ).append(
                                json(fullName)
                        ).append("\",");

                        menteesJson.append(
                                "\"email\":\""
                        ).append(
                                json(email)
                        ).append("\",");

                        menteesJson.append(
                                "\"mobile\":\""
                        ).append(
                                json(mobile)
                        ).append("\",");

                        menteesJson.append(
                                "\"profilePicture\":\""
                        ).append(
                                json(profilePicture)
                        ).append("\",");

                        menteesJson.append(
                                "\"totalSessions\":"
                        ).append(
                                totalSessions
                        ).append(",");

                        menteesJson.append(
                                "\"completedSessions\":"
                        ).append(
                                completedSessions
                        ).append(",");

                        menteesJson.append(
                                "\"lastSession\":\""
                        ).append(
                                json(lastSession)
                        ).append("\",");

                        menteesJson.append(
                                "\"nextSession\":\""
                        ).append(
                                json(nextSession)
                        ).append("\"");

                        menteesJson.append("}");
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
                        "message": "Unable to load mentees."
                    }
                    """);

                return;
            }

            menteesJson.append("]");

            // --------------------------------------------------------
            // FINAL RESPONSE
            // --------------------------------------------------------

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            out.println("{");

            out.println(
                    "\"success\":true,"
            );

            out.println(
                    "\"totalMentees\":"
                    + totalMentees
                    + ","
            );

            out.println(
                    "\"mentees\":"
                    + menteesJson
            );

            out.println("}");
        }
    }


    /*
     * ============================================================
     * GET SINGLE MENTEE
     * ============================================================
     *
     * GET:
     * /api/expert/mentees?studentId=5
     *
     * This is useful when expert clicks
     * "View Mentee".
     */

    private void getSingleMentee(
            int expertId,
            int studentId,
            PrintWriter out,
            HttpServletResponse response) {

        String sql = """
            SELECT
                u.user_id,
                u.first_name,
                u.last_name,
                u.email,
                u.mobile,
                u.profile_picture,

                COUNT(eb.booking_id) AS total_sessions,

                SUM(
                    CASE
                        WHEN eb.status = 'COMPLETED'
                        THEN 1
                        ELSE 0
                    END
                ) AS completed_sessions,

                SUM(
                    CASE
                        WHEN eb.status = 'CANCELLED'
                        THEN 1
                        ELSE 0
                    END
                ) AS cancelled_sessions,

                MAX(eb.scheduled_at) AS last_session,

                MIN(
                    CASE
                        WHEN eb.scheduled_at >= NOW()
                         AND eb.status IN ('PENDING', 'CONFIRMED')
                        THEN eb.scheduled_at
                        ELSE NULL
                    END
                ) AS next_session

            FROM expert_bookings eb

            INNER JOIN users u
                ON eb.student_id = u.user_id

            WHERE eb.expert_id = ?
              AND eb.student_id = ?
              AND u.role = 'STUDENT'

            GROUP BY
                u.user_id,
                u.first_name,
                u.last_name,
                u.email,
                u.mobile,
                u.profile_picture
            """;

        try (Connection connection =
                     DBConnection.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setInt(1, expertId);
            ps.setInt(2, studentId);

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (!rs.next()) {

                    response.setStatus(
                            HttpServletResponse.SC_NOT_FOUND
                    );

                    out.println("""
                        {
                            "success": false,
                            "message": "Mentee not found."
                        }
                        """);

                    return;
                }

                String firstName =
                        safe(
                            rs.getString("first_name")
                        );

                String lastName =
                        safe(
                            rs.getString("last_name")
                        );

                String fullName =
                        (firstName + " " + lastName)
                        .trim();

                out.println("{");

                out.println("\"success\":true,");

                out.println("\"mentee\":{");

                out.println(
                        "\"studentId\":"
                        + rs.getInt("user_id")
                        + ","
                );

                out.println(
                        "\"firstName\":\""
                        + json(firstName)
                        + "\","
                );

                out.println(
                        "\"lastName\":\""
                        + json(lastName)
                        + "\","
                );

                out.println(
                        "\"fullName\":\""
                        + json(fullName)
                        + "\","
                );

                out.println(
                        "\"email\":\""
                        + json(
                            safe(
                                rs.getString("email")
                            )
                        )
                        + "\","
                );

                out.println(
                        "\"mobile\":\""
                        + json(
                            safe(
                                rs.getString("mobile")
                            )
                        )
                        + "\","
                );

                out.println(
                        "\"profilePicture\":\""
                        + json(
                            safe(
                                rs.getString(
                                    "profile_picture"
                                )
                            )
                        )
                        + "\","
                );

                out.println(
                        "\"totalSessions\":"
                        + rs.getInt("total_sessions")
                        + ","
                );

                out.println(
                        "\"completedSessions\":"
                        + rs.getInt(
                            "completed_sessions"
                        )
                        + ","
                );

                out.println(
                        "\"cancelledSessions\":"
                        + rs.getInt(
                            "cancelled_sessions"
                        )
                        + ","
                );

                out.println(
                        "\"lastSession\":\""
                        + json(
                            safe(
                                rs.getString(
                                    "last_session"
                                )
                            )
                        )
                        + "\","
                );

                out.println(
                        "\"nextSession\":\""
                        + json(
                            safe(
                                rs.getString(
                                    "next_session"
                                )
                            )
                        )
                        + "\""
                );

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
                    "message": "Unable to load mentee details."
                }
                """);
        }
    }


    /*
     * ============================================================
     * REQUEST ROUTING FOR SINGLE MENTEE
     * ============================================================
     *
     * If studentId is supplied:
     *
     * /api/expert/mentees?studentId=5
     *
     * Otherwise:
     *
     * /api/expert/mentees
     *
     * returns all mentees.
     */

    private boolean hasStudentId(
            HttpServletRequest request) {

        String studentId =
                request.getParameter("studentId");

        return studentId != null
                && !studentId.isBlank();
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
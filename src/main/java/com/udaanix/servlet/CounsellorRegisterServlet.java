package com.udaanix.servlet;

import com.udaanix.config.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet("/api/auth/counsellor/register")
public class CounsellorRegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        // =====================================================
        // 1. GET DATA FROM COUNSELLOR REGISTRATION FORM
        // =====================================================

        String firstName =
                request.getParameter("firstName");

        String lastName =
                request.getParameter("lastName");

        String email =
                request.getParameter("email");

        String mobile =
                request.getParameter("mobile");

        String qualification =
                request.getParameter("qualification");

        String specialization =
                request.getParameter("specialization");

        String experience =
                request.getParameter("experience");

        String city =
                request.getParameter("city");

        String password =
                request.getParameter("password");

        try (PrintWriter out = response.getWriter()) {

            // =================================================
            // 2. BASIC VALIDATION
            // =================================================

            if (firstName == null
                    || lastName == null
                    || email == null
                    || mobile == null
                    || qualification == null
                    || specialization == null
                    || experience == null
                    || city == null
                    || password == null
                    || firstName.isBlank()
                    || lastName.isBlank()
                    || email.isBlank()
                    || mobile.isBlank()
                    || qualification.isBlank()
                    || specialization.isBlank()
                    || experience.isBlank()
                    || city.isBlank()
                    || password.isBlank()) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Please fill all required fields."
                    }
                    """);

                return;
            }

            // =================================================
            // 3. CLEAN INPUT
            // =================================================

            firstName = firstName.trim();
            lastName = lastName.trim();
            email = email.trim().toLowerCase();
            mobile = mobile.trim();

            qualification = qualification.trim();
            specialization = specialization.trim();
            experience = experience.trim();
            city = city.trim();

            // =================================================
            // 4. CONVERT EXPERIENCE RANGE TO INTEGER
            // =================================================

            int experienceYears;

            switch (experience) {

                case "0–2 Years":
                    experienceYears = 0;
                    break;

                case "3–5 Years":
                    experienceYears = 3;
                    break;

                case "6–10 Years":
                    experienceYears = 6;
                    break;

                case "11–15 Years":
                    experienceYears = 11;
                    break;

                case "15+ Years":
                    experienceYears = 15;
                    break;

                default:
                    response.setStatus(
                            HttpServletResponse.SC_BAD_REQUEST
                    );

                    out.println("""
                        {
                            "success": false,
                            "message": "Invalid experience selection."
                        }
                        """);

                    return;
            }

            // =================================================
            // 5. PASSWORD VALIDATION
            // =================================================

            if (password.length() < 8) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Password must contain at least 8 characters."
                    }
                    """);

                return;
            }

            // =================================================
            // 6. DATABASE CONNECTION
            // =================================================

            try (Connection connection =
                         DBConnection.getConnection()) {

                /*
                 * Both users and counsellors are inserted
                 * inside the same transaction.
                 */

                connection.setAutoCommit(false);

                try {

                    // =============================================
                    // 7. CHECK DUPLICATE EMAIL
                    // =============================================

                    String checkEmailSQL =
                            "SELECT user_id "
                            + "FROM users "
                            + "WHERE email = ?";

                    try (PreparedStatement checkStmt =
                                 connection.prepareStatement(
                                         checkEmailSQL)) {

                        checkStmt.setString(1, email);

                        try (ResultSet rs =
                                     checkStmt.executeQuery()) {

                            if (rs.next()) {

                                connection.rollback();

                                response.setStatus(
                                        HttpServletResponse.SC_CONFLICT
                                );

                                out.println("""
                                    {
                                        "success": false,
                                        "message": "An account with this email already exists."
                                    }
                                    """);

                                return;
                            }
                        }
                    }

                    // =============================================
                    // 8. HASH PASSWORD
                    // =============================================

                    String passwordHash =
                            BCrypt.hashpw(
                                    password,
                                    BCrypt.gensalt(12)
                            );

                    // =============================================
                    // 9. INSERT INTO USERS
                    // =============================================

                    String userSQL = """
                        INSERT INTO users
                        (
                            role,
                            first_name,
                            last_name,
                            email,
                            mobile,
                            password_hash
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;

                    long userId;

                    try (PreparedStatement userStmt =
                                 connection.prepareStatement(
                                         userSQL,
                                         Statement.RETURN_GENERATED_KEYS)) {

                        userStmt.setString(1, "COUNSELLOR");
                        userStmt.setString(2, firstName);
                        userStmt.setString(3, lastName);
                        userStmt.setString(4, email);
                        userStmt.setString(5, mobile);
                        userStmt.setString(6, passwordHash);

                        userStmt.executeUpdate();

                        // =============================================
                        // GET GENERATED USER ID
                        // =============================================

                        try (ResultSet generatedKeys =
                                     userStmt.getGeneratedKeys()) {

                            if (!generatedKeys.next()) {

                                throw new Exception(
                                        "Could not create counsellor account."
                                );
                            }

                            userId =
                                    generatedKeys.getLong(1);
                        }
                    }

                    // =============================================
                    // 10. INSERT INTO COUNSELLORS
                    // =============================================

                    /*
                     * counsellor_id = users.user_id
                     */

                    String counsellorSQL = """
                        INSERT INTO counsellors
                        (
                            counsellor_id,
                            qualification,
                            specialization,
                            experience_years,
                            city
                        )
                        VALUES (?, ?, ?, ?, ?)
                        """;

                    try (PreparedStatement counsellorStmt =
                                 connection.prepareStatement(
                                         counsellorSQL)) {

                        counsellorStmt.setLong(
                                1,
                                userId
                        );

                        counsellorStmt.setString(
                                2,
                                qualification
                        );

                        counsellorStmt.setString(
                                3,
                                specialization
                        );

                        counsellorStmt.setInt(
                                4,
                                experienceYears
                        );

                        counsellorStmt.setString(
                                5,
                                city
                        );

                        counsellorStmt.executeUpdate();
                    }

                    // =============================================
                    // 11. COMMIT
                    // =============================================

                    connection.commit();

                    // =============================================
                    // 12. SUCCESS RESPONSE
                    // =============================================

                    response.setStatus(
                            HttpServletResponse.SC_CREATED
                    );

                    out.println("""
                        {
                            "success": true,
                            "message": "Counsellor account created successfully."
                        }
                        """);

                } catch (Exception e) {

                    // Roll back both users and counsellors
                    connection.rollback();

                    throw e;
                }

            } catch (Exception e) {

                e.printStackTrace();

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Registration failed. Please try again."
                    }
                    """);
            }
        }
    }
}
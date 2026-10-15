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

@WebServlet("/api/auth/register")
public class AuthRegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        // =====================================================
        // 1. GET DATA FROM STUDENT REGISTRATION FORM
        // =====================================================

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("email");
        String mobile = request.getParameter("mobile");

        String educationLevel =
                request.getParameter("educationLevel");

        String qualification =
                request.getParameter("qualification");

        String city = request.getParameter("city");
        String bio = request.getParameter("bio");

        String password = request.getParameter("password");

        try (PrintWriter out = response.getWriter()) {

            // =================================================
            // 2. BASIC VALIDATION
            // =================================================

            if (firstName == null
                    || lastName == null
                    || email == null
                    || mobile == null
                    || educationLevel == null
                    || qualification == null
                    || city == null
                    || password == null
                    || firstName.isBlank()
                    || lastName.isBlank()
                    || email.isBlank()
                    || mobile.isBlank()
                    || educationLevel.isBlank()
                    || qualification.isBlank()
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

            educationLevel = educationLevel.trim();
            qualification = qualification.trim();
            city = city.trim();

            if (bio == null) {
                bio = "";
            } else {
                bio = bio.trim();
            }

            // =================================================
            // 4. PASSWORD VALIDATION
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
            // 5. DATABASE CONNECTION
            // =================================================

            try (Connection connection =
                         DBConnection.getConnection()) {

                /*
                 * users and students must both be inserted.
                 *
                 * If any operation fails,
                 * everything will be rolled back.
                 */

                connection.setAutoCommit(false);

                try {

                    // =============================================
                    // 6. CHECK DUPLICATE EMAIL
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
                    // 7. HASH PASSWORD
                    // =============================================

                    String passwordHash =
                            BCrypt.hashpw(
                                    password,
                                    BCrypt.gensalt(12)
                            );

                    // =============================================
                    // 8. INSERT INTO USERS
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

                        // Role is fixed for this registration
                        userStmt.setString(1, "STUDENT");

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
                                        "Could not create user account."
                                );
                            }

                            userId =
                                    generatedKeys.getLong(1);
                        }
                    }

                    // =============================================
                    // 9. INSERT INTO STUDENTS
                    // =============================================

                    /*
                     * students.student_id uses the same ID
                     * generated in users.user_id.
                     */

                    String studentSQL = """
                        INSERT INTO students
                        (
                            student_id,
                            education_level,
                            qualification,
                            bio,
                            city
                        )
                        VALUES (?, ?, ?, ?, ?)
                        """;

                    try (PreparedStatement studentStmt =
                                 connection.prepareStatement(
                                         studentSQL)) {

                        // Link student profile to user account
                        studentStmt.setLong(1, userId);

                        // Student-specific information
                        studentStmt.setString(2, educationLevel);
                        studentStmt.setString(3, qualification);
                        studentStmt.setString(4, bio);
                        studentStmt.setString(5, city);

                        studentStmt.executeUpdate();
                    }

                    // =============================================
                    // 10. COMMIT TRANSACTION
                    // =============================================

                    connection.commit();

                    // =============================================
                    // 11. SUCCESS RESPONSE
                    // =============================================

                    response.setStatus(
                            HttpServletResponse.SC_CREATED
                    );

                    out.println("""
                        {
                            "success": true,
                            "message": "Student account created successfully."
                        }
                        """);

                } catch (Exception e) {

                    // If anything fails, undo both inserts
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
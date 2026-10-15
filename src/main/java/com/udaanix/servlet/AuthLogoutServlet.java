package com.udaanix.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Common Authentication Logout Servlet
 *
 * Endpoint:
 * POST /api/auth/logout
 *
 * Also supports GET for simple browser/logout fallback.
 *
 * The current session is invalidated so the logged-in
 * user can no longer access protected APIs.
 */
@WebServlet("/api/auth/logout")
public class AuthLogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // POST LOGOUT
    // =========================================================

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        logout(request, response);
    }

    // =========================================================
    // GET LOGOUT
    // =========================================================
    //
    // This is useful as a fallback if a logout link is opened
    // directly from the browser.
    // =========================================================

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        logout(request, response);
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void logout(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out =
                response.getWriter();

        // -----------------------------------------------------
        // GET CURRENT SESSION
        // -----------------------------------------------------

        HttpSession session =
                request.getSession(false);

        // -----------------------------------------------------
        // INVALIDATE SESSION
        // -----------------------------------------------------

        if (session != null) {
            session.invalidate();
        }

        // -----------------------------------------------------
        // RESPONSE
        // -----------------------------------------------------

        out.print(
            "{"
            + "\"success\":true,"
            + "\"message\":\"Logged out successfully.\""
            + "}"
        );
    }
}
package com.udaanix.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/expert/logout")
public class ExpertLogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            HttpSession session =
                    request.getSession(false);

            if (session != null) {

                // Invalidate server-side session
                session.invalidate();
            }

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            out.print("{");
            out.print("\"success\":true,");
            out.print("\"message\":\"Logged out successfully.\"");
            out.print("}");

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            try (PrintWriter out =
                         response.getWriter()) {

                out.print("{");
                out.print("\"success\":false,");
                out.print("\"message\":\"Logout failed. Please try again.\"");
                out.print("}");
            }
        }
    }
}
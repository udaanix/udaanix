package com.udaanix.servlet;

import com.udaanix.config.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;

@WebServlet("/test-db")
public class TestDBServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        try (Connection connection = DBConnection.getConnection();
             PrintWriter out = response.getWriter()) {

            out.println("<html>");
            out.println("<head>");
            out.println("<title>Udaanix Database Test</title>");
            out.println("</head>");
            out.println("<body>");

            out.println("<h1>Udaanix Backend</h1>");
            out.println("<h2>Database Connected Successfully! ✅</h2>");

            out.println("<p>Database: udaanix_db</p>");
            out.println("<p>MySQL Connection: SUCCESS</p>");

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            try (PrintWriter out = response.getWriter()) {

                out.println("<html>");
                out.println("<head>");
                out.println("<title>Database Error</title>");
                out.println("</head>");
                out.println("<body>");

                out.println("<h1>Database Connection Failed ❌</h1>");
                out.println("<p>" + e.getMessage() + "</p>");

                out.println("</body>");
                out.println("</html>");
            }
        }
    }
}
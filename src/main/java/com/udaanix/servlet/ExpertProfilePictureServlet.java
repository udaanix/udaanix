package com.udaanix.servlet;

import com.udaanix.config.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@WebServlet("/api/expert/profile-picture")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 6 * 1024 * 1024
)
public class ExpertProfilePictureServlet extends HttpServlet {

    /*
     * ============================================================
     * UPLOAD PROFILE PICTURE
     * ============================================================
     *
     * POST:
     * /api/expert/profile-picture
     *
     * Multipart field:
     * profilePicture
     */

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            // --------------------------------------------------------
            // CHECK LOGIN SESSION
            // --------------------------------------------------------

            HttpSession session = request.getSession(false);

            if (session == null
                    || session.getAttribute("expertId") == null
                    || !"EXPERT".equals(session.getAttribute("role"))) {

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
            // GET UPLOADED FILE
            // --------------------------------------------------------

            Part filePart =
                    request.getPart("profilePicture");

            if (filePart == null
                    || filePart.getSize() == 0) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Please select a profile picture."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // FILE SIZE CHECK
            // --------------------------------------------------------

            long fileSize = filePart.getSize();

            if (fileSize > 5 * 1024 * 1024) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Profile picture must be smaller than 5 MB."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // CONTENT TYPE CHECK
            // --------------------------------------------------------

            String contentType =
                    filePart.getContentType();

            if (contentType == null
                    || !(contentType.equalsIgnoreCase("image/jpeg")
                    || contentType.equalsIgnoreCase("image/png")
                    || contentType.equalsIgnoreCase("image/webp"))) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Only JPG, PNG and WEBP images are allowed."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // FILE EXTENSION
            // --------------------------------------------------------

            String extension;

            if (contentType.equalsIgnoreCase("image/png")) {

                extension = ".png";

            } else if (contentType.equalsIgnoreCase("image/webp")) {

                extension = ".webp";

            } else {

                extension = ".jpg";
            }

            // --------------------------------------------------------
            // UNIQUE FILE NAME
            // --------------------------------------------------------

            String fileName =
                    "expert_"
                    + expertId
                    + "_"
                    + UUID.randomUUID()
                    + extension;

            // --------------------------------------------------------
            // UPLOAD DIRECTORY
            // --------------------------------------------------------

            String uploadDirectory =
                    getServletContext().getRealPath(
                            "/uploads/experts"
                    );

            if (uploadDirectory == null) {

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Unable to create upload directory."
                    }
                    """);

                return;
            }

            Path uploadPath =
                    Paths.get(uploadDirectory);

            Files.createDirectories(uploadPath);

            Path destination =
                    uploadPath.resolve(fileName);

            // --------------------------------------------------------
            // GET OLD PICTURE
            // --------------------------------------------------------

            String oldPicture = "";

            String oldPictureSQL = """
                SELECT
                    e.profile_picture,
                    u.profile_picture AS user_profile_picture
                FROM experts e
                INNER JOIN users u
                    ON e.expert_id = u.user_id
                WHERE e.expert_id = ?
                  AND u.role = 'EXPERT'
                """;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(oldPictureSQL)) {

                ps.setInt(1, expertId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        oldPicture =
                                safe(rs.getString("profile_picture"));

                        if (oldPicture.isEmpty()) {

                            oldPicture =
                                    safe(
                                        rs.getString(
                                            "user_profile_picture"
                                        )
                                    );
                        }
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
                        "message": "Unable to read existing profile picture."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // SAVE NEW FILE
            // --------------------------------------------------------

            try (InputStream inputStream =
                         filePart.getInputStream()) {

                Files.copy(
                        inputStream,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );

            } catch (Exception e) {

                e.printStackTrace();

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                );

                out.println("""
                    {
                        "success": false,
                        "message": "Unable to save profile picture."
                    }
                    """);

                return;
            }

            /*
             * Store relative path in database.
             *
             * Example:
             *
             * uploads/experts/expert_5_xxxxx.jpg
             */

            String picturePath =
                    "uploads/experts/" + fileName;

            // --------------------------------------------------------
            // UPDATE DATABASE
            // --------------------------------------------------------

            try (Connection connection =
                         DBConnection.getConnection()) {

                connection.setAutoCommit(false);

                try {

                    // ------------------------------------------------
                    // UPDATE EXPERTS TABLE
                    // ------------------------------------------------

                    String expertSQL = """
                        UPDATE experts
                        SET
                            profile_picture = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE expert_id = ?
                        """;

                    try (PreparedStatement ps =
                                 connection.prepareStatement(expertSQL)) {

                        ps.setString(1, picturePath);
                        ps.setInt(2, expertId);

                        ps.executeUpdate();
                    }

                    // ------------------------------------------------
                    // UPDATE USERS TABLE
                    // ------------------------------------------------

                    String userSQL = """
                        UPDATE users
                        SET
                            profile_picture = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = ?
                          AND role = 'EXPERT'
                        """;

                    try (PreparedStatement ps =
                                 connection.prepareStatement(userSQL)) {

                        ps.setString(1, picturePath);
                        ps.setInt(2, expertId);

                        ps.executeUpdate();
                    }

                    // ------------------------------------------------
                    // COMMIT
                    // ------------------------------------------------

                    connection.commit();

                } catch (Exception e) {

                    try {
                        connection.rollback();
                    } catch (Exception rollbackError) {
                        rollbackError.printStackTrace();
                    }

                    // Remove newly uploaded file because
                    // database update failed.

                    try {
                        Files.deleteIfExists(destination);
                    } catch (Exception fileError) {
                        fileError.printStackTrace();
                    }

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
                        "message": "Unable to update profile picture in database."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // UPDATE SESSION
            // --------------------------------------------------------

            session.setAttribute(
                    "expertProfilePicture",
                    picturePath
            );

            // --------------------------------------------------------
            // DELETE OLD FILE
            // --------------------------------------------------------

            deleteOldPicture(
                    oldPicture,
                    uploadPath
            );

            // --------------------------------------------------------
            // SUCCESS
            // --------------------------------------------------------

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            out.println("{");

            out.println("\"success\":true,");

            out.println(
                    "\"message\":\"Profile picture uploaded successfully.\","
            );

            out.println(
                    "\"profilePicture\":\""
                    + json(picturePath)
                    + "\""
            );

            out.println("}");
        }
    }


    /*
     * ============================================================
     * REMOVE PROFILE PICTURE
     * ============================================================
     *
     * DELETE:
     * /api/expert/profile-picture
     */

    @Override
    protected void doDelete(HttpServletRequest request,
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
                    || !"EXPERT".equals(session.getAttribute("role"))) {

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
            // GET CURRENT PICTURE
            // --------------------------------------------------------

            String oldPicture = "";

            String selectSQL = """
                SELECT profile_picture
                FROM experts
                WHERE expert_id = ?
                """;

            try (Connection connection =
                         DBConnection.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(selectSQL)) {

                ps.setInt(1, expertId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {

                        oldPicture =
                                safe(
                                    rs.getString(
                                        "profile_picture"
                                    )
                                );
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
                        "message": "Unable to read current profile picture."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // REMOVE FROM DATABASE
            // --------------------------------------------------------

            try (Connection connection =
                         DBConnection.getConnection()) {

                connection.setAutoCommit(false);

                try {

                    // ------------------------------------------------
                    // CLEAR EXPERTS TABLE
                    // ------------------------------------------------

                    String expertSQL = """
                        UPDATE experts
                        SET
                            profile_picture = NULL,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE expert_id = ?
                        """;

                    try (PreparedStatement ps =
                                 connection.prepareStatement(expertSQL)) {

                        ps.setInt(1, expertId);

                        ps.executeUpdate();
                    }

                    // ------------------------------------------------
                    // CLEAR USERS TABLE
                    // ------------------------------------------------

                    String userSQL = """
                        UPDATE users
                        SET
                            profile_picture = NULL,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = ?
                          AND role = 'EXPERT'
                        """;

                    try (PreparedStatement ps =
                                 connection.prepareStatement(userSQL)) {

                        ps.setInt(1, expertId);

                        ps.executeUpdate();
                    }

                    // ------------------------------------------------
                    // COMMIT
                    // ------------------------------------------------

                    connection.commit();

                } catch (Exception e) {

                    try {
                        connection.rollback();
                    } catch (Exception rollbackError) {
                        rollbackError.printStackTrace();
                    }

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
                        "message": "Unable to remove profile picture."
                    }
                    """);

                return;
            }

            // --------------------------------------------------------
            // DELETE OLD IMAGE FILE
            // --------------------------------------------------------

            try {

                String uploadDirectory =
                        getServletContext().getRealPath(
                                "/uploads/experts"
                        );

                if (uploadDirectory != null) {

                    Path uploadPath =
                            Paths.get(uploadDirectory);

                    deleteOldPicture(
                            oldPicture,
                            uploadPath
                    );
                }

            } catch (Exception e) {

                /*
                 * Database update is already successful.
                 * Physical file deletion failure should not
                 * make the API fail.
                 */

                System.err.println(
                        "Could not delete old expert profile picture: "
                        + e.getMessage()
                );
            }

            // --------------------------------------------------------
            // CLEAR SESSION
            // --------------------------------------------------------

            session.setAttribute(
                    "expertProfilePicture",
                    ""
            );

            // --------------------------------------------------------
            // SUCCESS
            // --------------------------------------------------------

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            out.println("""
                {
                    "success": true,
                    "message": "Profile picture removed successfully."
                }
                """);
        }
    }


    /*
     * ============================================================
     * DELETE OLD PICTURE SAFELY
     * ============================================================
     */

    private void deleteOldPicture(
            String oldPicture,
            Path uploadPath) {

        try {

            if (oldPicture == null
                    || oldPicture.isBlank()) {

                return;
            }

            /*
             * Only delete files that belong to
             * the Udaanix expert upload folder.
             */

            if (!oldPicture.startsWith(
                    "uploads/experts/")) {

                return;
            }

            String fileName =
                    oldPicture.substring(
                            "uploads/experts/".length()
                    );

            Path oldFile =
                    uploadPath
                            .resolve(fileName)
                            .normalize();

            Path normalizedUploadPath =
                    uploadPath
                            .toAbsolutePath()
                            .normalize();

            /*
             * Security check.
             *
             * Prevent deleting a file outside
             * the expert upload directory.
             */

            if (!oldFile
                    .toAbsolutePath()
                    .normalize()
                    .startsWith(normalizedUploadPath)) {

                return;
            }

            Files.deleteIfExists(oldFile);

        } catch (Exception e) {

            System.err.println(
                    "Could not delete old expert profile picture: "
                    + e.getMessage()
            );
        }
    }


    /*
     * ============================================================
     * NULL-SAFE STRING
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
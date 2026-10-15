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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * Student Profile Picture Servlet
 *
 * POST /api/student/profile-picture
 *      Upload/update logged-in student's profile picture.
 *
 * GET /api/student/profile-picture
 *      Display logged-in student's profile picture.
 *
 * DELETE /api/student/profile-picture
 *      Remove logged-in student's profile picture.
 */
@WebServlet("/api/student/profile-picture")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 6 * 1024 * 1024
)
public class StudentProfilePictureServlet extends HttpServlet {

    /*
     * =========================================================
     * UPLOAD DIRECTORY
     *
     * We intentionally keep uploaded files outside the WAR.
     * This prevents profile pictures from disappearing every
     * time the application is redeployed.
     * =========================================================
     */
    private Path uploadDirectory;


    @Override
    public void init() throws ServletException {

        try {

            String userHome =
                    System.getProperty("user.home");

            uploadDirectory =
                    Paths.get(
                            userHome,
                            "Udaanix",
                            "uploads",
                            "profile-pictures"
                    );

            Files.createDirectories(
                    uploadDirectory
            );

            System.out.println(
                    "Udaanix profile picture directory: "
                    + uploadDirectory.toAbsolutePath()
            );

        } catch (Exception e) {

            throw new ServletException(
                    "Unable to create profile picture directory.",
                    e
            );
        }
    }


    /*
     * =========================================================
     * POST
     *
     * Upload / replace profile picture.
     * =========================================================
     */

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );


        /*
         * -----------------------------------------------------
         * GET SESSION
         * -----------------------------------------------------
         */

        HttpSession session =
                request.getSession(false);


        Long userId =
                getLoggedInUserId(session);


        if (userId == null) {

            sendJson(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    false,
                    "Student is not logged in."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * CHECK ROLE
         * -----------------------------------------------------
         */

        String role =
                session != null
                        ? (String) session.getAttribute("role")
                        : null;


        if (role != null &&
            !"STUDENT".equalsIgnoreCase(role)) {

            sendJson(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    false,
                    "Access denied."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * GET FILE
         * -----------------------------------------------------
         */

        Part filePart;

        try {

            filePart =
                    request.getPart("profilePicture");

        } catch (Exception e) {

            sendJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Please select a profile picture."
            );

            return;
        }


        if (filePart == null ||
            filePart.getSize() == 0) {

            sendJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Please select a profile picture."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * FILE SIZE
         * -----------------------------------------------------
         */

        if (filePart.getSize() >
                5L * 1024L * 1024L) {

            sendJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Profile picture must be smaller than 5 MB."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * CHECK MIME TYPE
         * -----------------------------------------------------
         */

        String contentType =
                filePart.getContentType();


        if (contentType == null ||
            !isAllowedImageType(contentType)) {

            sendJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Only JPG, JPEG, PNG, WEBP or GIF images are allowed."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * EXTENSION
         * -----------------------------------------------------
         */

        String extension =
                getExtension(contentType);


        if (extension == null) {

            sendJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Unsupported image format."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * CREATE UNIQUE FILE NAME
         *
         * Example:
         *
         * student_12_a83f...jpg
         * -----------------------------------------------------
         */

        String fileName =
                "student_"
                + userId
                + "_"
                + UUID.randomUUID()
                + extension;


        Path newFile =
                uploadDirectory.resolve(
                        fileName
                );


        /*
         * -----------------------------------------------------
         * SAVE FILE
         * -----------------------------------------------------
         */

        try (InputStream input =
                     filePart.getInputStream()) {

            Files.copy(
                    input,
                    newFile,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }


        /*
         * -----------------------------------------------------
         * GET OLD PROFILE PICTURE
         * -----------------------------------------------------
         */

        String oldFileName = null;


        String selectSql = """
            SELECT profile_picture
            FROM users
            WHERE user_id = ?
              AND role = 'STUDENT'
            """;


        try (
            Connection con =
                    DBConnection.getConnection();

            PreparedStatement ps =
                    con.prepareStatement(selectSql)
        ) {

            ps.setLong(
                    1,
                    userId
            );


            try (
                ResultSet rs =
                        ps.executeQuery()
            ) {

                if (rs.next()) {

                    oldFileName =
                            rs.getString(
                                    "profile_picture"
                            );
                }
            }

        } catch (Exception e) {

            /*
             * If database read fails, remove the newly
             * uploaded file so we don't leave orphan files.
             */

            deleteQuietly(newFile);

            e.printStackTrace();

            sendJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Unable to access student profile."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * UPDATE DATABASE
         * -----------------------------------------------------
         */

        String updateSql = """
            UPDATE users
            SET profile_picture = ?
            WHERE user_id = ?
              AND role = 'STUDENT'
            """;


        try (
            Connection con =
                    DBConnection.getConnection();

            PreparedStatement ps =
                    con.prepareStatement(updateSql)
        ) {

            ps.setString(
                    1,
                    fileName
            );

            ps.setLong(
                    2,
                    userId
            );


            int updated =
                    ps.executeUpdate();


            if (updated == 0) {

                deleteQuietly(newFile);

                sendJson(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        false,
                        "Student account not found."
                );

                return;
            }


        } catch (Exception e) {

            /*
             * Database update failed.
             * Delete newly uploaded file.
             */

            deleteQuietly(newFile);

            e.printStackTrace();

            sendJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Unable to save profile picture."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * DELETE OLD FILE
         *
         * Only delete the old filename belonging to the
         * Udaanix profile-picture folder.
         * -----------------------------------------------------
         */

        if (oldFileName != null &&
            !oldFileName.isBlank()) {

            deleteOldProfilePicture(
                    oldFileName
            );
        }


        /*
         * -----------------------------------------------------
         * SUCCESS
         * -----------------------------------------------------
         */

        sendJson(
                response,
                HttpServletResponse.SC_OK,
                true,
                "Profile picture updated successfully."
        );
    }


    /*
     * =========================================================
     * GET
     *
     * Display current logged-in student's image.
     * =========================================================
     */

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {


        /*
         * -----------------------------------------------------
         * GET SESSION
         * -----------------------------------------------------
         */

        HttpSession session =
                request.getSession(false);


        Long userId =
                getLoggedInUserId(session);


        if (userId == null) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * FIND PROFILE PICTURE
         * -----------------------------------------------------
         */

        String fileName = null;


        String sql = """
            SELECT profile_picture
            FROM users
            WHERE user_id = ?
              AND role = 'STUDENT'
              AND is_active = TRUE
            """;


        try (
            Connection con =
                    DBConnection.getConnection();

            PreparedStatement ps =
                    con.prepareStatement(sql)
        ) {

            ps.setLong(
                    1,
                    userId
            );


            try (
                ResultSet rs =
                        ps.executeQuery()
            ) {

                if (!rs.next()) {

                    response.setStatus(
                            HttpServletResponse.SC_NOT_FOUND
                    );

                    return;
                }


                fileName =
                        rs.getString(
                                "profile_picture"
                        );
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * NO PROFILE PICTURE
         * -----------------------------------------------------
         */

        if (fileName == null ||
            fileName.isBlank()) {

            response.setStatus(
                    HttpServletResponse.SC_NOT_FOUND
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * SECURITY
         *
         * Only allow simple file names.
         * Prevent path traversal.
         * -----------------------------------------------------
         */

        if (!isSafeFileName(fileName)) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * FILE PATH
         * -----------------------------------------------------
         */

        Path imagePath =
                uploadDirectory.resolve(
                        fileName
                ).normalize();


        /*
         * Make sure resolved path remains inside
         * our profile-picture directory.
         */

        if (!imagePath.startsWith(
                uploadDirectory.normalize())) {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            return;
        }


        if (!Files.exists(imagePath) ||
            !Files.isRegularFile(imagePath)) {

            response.setStatus(
                    HttpServletResponse.SC_NOT_FOUND
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * CONTENT TYPE
         * -----------------------------------------------------
         */

        String contentType =
                Files.probeContentType(
                        imagePath
                );


        if (contentType == null) {

            contentType =
                    getContentTypeFromExtension(
                            fileName
                    );
        }


        response.setContentType(
                contentType
        );


        /*
         * -----------------------------------------------------
         * CACHE
         *
         * Small cache so browser doesn't keep an old image.
         * Dashboard can add ?v=timestamp.
         * -----------------------------------------------------
         */

        response.setHeader(
                "Cache-Control",
                "private, max-age=3600"
        );


        /*
         * -----------------------------------------------------
         * STREAM IMAGE
         * -----------------------------------------------------
         */

        try (
            InputStream input =
                    Files.newInputStream(imagePath);

            OutputStream output =
                    response.getOutputStream()
        ) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;


            while ((bytesRead =
                    input.read(buffer)) != -1) {

                output.write(
                        buffer,
                        0,
                        bytesRead
                );
            }
        }
    }


    /*
     * =========================================================
     * DELETE
     *
     * Remove current profile picture.
     * =========================================================
     */

    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );


        HttpSession session =
                request.getSession(false);


        Long userId =
                getLoggedInUserId(session);


        if (userId == null) {

            sendJson(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    false,
                    "Student is not logged in."
            );

            return;
        }


        String oldFileName = null;


        /*
         * -----------------------------------------------------
         * GET CURRENT FILE
         * -----------------------------------------------------
         */

        String selectSql = """
            SELECT profile_picture
            FROM users
            WHERE user_id = ?
              AND role = 'STUDENT'
            """;


        try (
            Connection con =
                    DBConnection.getConnection();

            PreparedStatement ps =
                    con.prepareStatement(selectSql)
        ) {

            ps.setLong(
                    1,
                    userId
            );


            try (
                ResultSet rs =
                        ps.executeQuery()
            ) {

                if (!rs.next()) {

                    sendJson(
                            response,
                            HttpServletResponse.SC_NOT_FOUND,
                            false,
                            "Student account not found."
                    );

                    return;
                }


                oldFileName =
                        rs.getString(
                                "profile_picture"
                        );
            }


        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Unable to load profile picture."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * REMOVE DATABASE REFERENCE
         * -----------------------------------------------------
         */

        String updateSql = """
            UPDATE users
            SET profile_picture = NULL
            WHERE user_id = ?
              AND role = 'STUDENT'
            """;


        try (
            Connection con =
                    DBConnection.getConnection();

            PreparedStatement ps =
                    con.prepareStatement(updateSql)
        ) {

            ps.setLong(
                    1,
                    userId
            );


            ps.executeUpdate();


        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Unable to remove profile picture."
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * DELETE ACTUAL FILE
         * -----------------------------------------------------
         */

        if (oldFileName != null &&
            !oldFileName.isBlank()) {

            deleteOldProfilePicture(
                    oldFileName
            );
        }


        sendJson(
                response,
                HttpServletResponse.SC_OK,
                true,
                "Profile picture removed successfully."
        );
    }


    /*
     * =========================================================
     * GET USER ID FROM SESSION
     * =========================================================
     */

    private Long getLoggedInUserId(
            HttpSession session) {

        if (session == null) {
            return null;
        }


        Object value =
                session.getAttribute("userId");


        if (!(value instanceof Number)) {
            return null;
        }


        return ((Number) value).longValue();
    }


    /*
     * =========================================================
     * CHECK ALLOWED IMAGE TYPE
     * =========================================================
     */

    private boolean isAllowedImageType(
            String contentType) {

        return "image/jpeg".equalsIgnoreCase(contentType)
                || "image/jpg".equalsIgnoreCase(contentType)
                || "image/png".equalsIgnoreCase(contentType)
                || "image/webp".equalsIgnoreCase(contentType)
                || "image/gif".equalsIgnoreCase(contentType);
    }


    /*
     * =========================================================
     * GET EXTENSION
     * =========================================================
     */

    private String getExtension(
            String contentType) {

        if ("image/jpeg".equalsIgnoreCase(contentType)
                || "image/jpg".equalsIgnoreCase(contentType)) {

            return ".jpg";
        }

        if ("image/png".equalsIgnoreCase(contentType)) {

            return ".png";
        }

        if ("image/webp".equalsIgnoreCase(contentType)) {

            return ".webp";
        }

        if ("image/gif".equalsIgnoreCase(contentType)) {

            return ".gif";
        }

        return null;
    }


    /*
     * =========================================================
     * FALLBACK CONTENT TYPE
     * =========================================================
     */

    private String getContentTypeFromExtension(
            String fileName) {

        String lower =
                fileName.toLowerCase();


        if (lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg")) {

            return "image/jpeg";
        }


        if (lower.endsWith(".png")) {

            return "image/png";
        }


        if (lower.endsWith(".webp")) {

            return "image/webp";
        }


        if (lower.endsWith(".gif")) {

            return "image/gif";
        }


        return "application/octet-stream";
    }


    /*
     * =========================================================
     * SAFE FILE NAME
     * =========================================================
     */

    private boolean isSafeFileName(
            String fileName) {

        if (fileName == null ||
            fileName.isBlank()) {

            return false;
        }


        return fileName.matches(
                "student_[0-9]+_[a-fA-F0-9-]+\\.(jpg|png|webp|gif)"
        );
    }


    /*
     * =========================================================
     * DELETE OLD PROFILE PICTURE
     * =========================================================
     */

    private void deleteOldProfilePicture(
            String oldFileName) {

        try {

            if (!isSafeFileName(oldFileName)) {
                return;
            }


            Path oldFile =
                    uploadDirectory
                            .resolve(oldFileName)
                            .normalize();


            if (!oldFile.startsWith(
                    uploadDirectory.normalize())) {

                return;
            }


            Files.deleteIfExists(
                    oldFile
            );


        } catch (Exception e) {

            /*
             * Don't break the successful DB operation
             * if old file deletion fails.
             */

            e.printStackTrace();
        }
    }


    /*
     * =========================================================
     * DELETE FILE QUIETLY
     * =========================================================
     */

    private void deleteQuietly(
            Path file) {

        try {

            if (file != null) {

                Files.deleteIfExists(file);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    /*
     * =========================================================
     * JSON RESPONSE
     * =========================================================
     */

    private void sendJson(
            HttpServletResponse response,
            int status,
            boolean success,
            String message)
            throws IOException {

        response.setStatus(status);

        response.getWriter().print(
                "{"
                + "\"success\":"
                + success
                + ","
                + "\"message\":\""
                + escapeJson(message)
                + "\""
                + "}"
        );
    }


    /*
     * =========================================================
     * JSON ESCAPE
     * =========================================================
     */

    private String escapeJson(
            String value) {

        if (value == null) {
            return "";
        }


        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
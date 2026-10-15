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

@WebServlet("/api/counsellor/profile-picture")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 6 * 1024 * 1024
)
public class CounsellorProfilePictureServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private Path uploadDirectory;


    // =========================================================
    // INITIALIZE UPLOAD DIRECTORY
    // =========================================================

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
                    "Counsellor profile picture directory: "
                    + uploadDirectory.toAbsolutePath()
            );

        } catch (Exception e) {

            throw new ServletException(
                    "Unable to create profile picture directory.",
                    e
            );
        }
    }


    // =========================================================
    // POST - UPLOAD / CHANGE PROFILE PICTURE
    // =========================================================

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

        HttpSession session =
                request.getSession(false);

        Long userId =
                getLoggedInUserId(session);

        // -----------------------------------------------------
        // LOGIN CHECK
        // -----------------------------------------------------

        if (userId == null) {

            sendJson(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    false,
                    "Counsellor is not logged in."
            );

            return;
        }

        // -----------------------------------------------------
        // ROLE CHECK
        // -----------------------------------------------------

        String role =
                session != null
                        ? (String) session.getAttribute("role")
                        : null;

        if (role == null ||
                !"COUNSELLOR".equalsIgnoreCase(role)) {

            sendJson(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    false,
                    "Access denied. Counsellor account required."
            );

            return;
        }

        // -----------------------------------------------------
        // GET FILE
        // -----------------------------------------------------

        Part filePart =
                request.getPart(
                        "profilePicture"
                );

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

        // -----------------------------------------------------
        // CHECK TYPE
        // -----------------------------------------------------

        String contentType =
                filePart.getContentType();

        if (!isAllowedImageType(
                contentType)) {

            sendJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Only JPG, PNG, WEBP and GIF images are allowed."
            );

            return;
        }

        // -----------------------------------------------------
        // FILE EXTENSION
        // -----------------------------------------------------

        String extension =
                getExtension(
                        filePart.getSubmittedFileName(),
                        contentType
                );

        // -----------------------------------------------------
        // UNIQUE FILE NAME
        // -----------------------------------------------------

        String newFileName =
                "counsellor_"
                + userId
                + "_"
                + UUID.randomUUID()
                + extension;

        Path newFile =
                uploadDirectory.resolve(
                        newFileName
                );

        // -----------------------------------------------------
        // SAVE NEW FILE
        // -----------------------------------------------------

        try (
                InputStream input =
                        filePart.getInputStream()
        ) {

            Files.copy(
                    input,
                    newFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Unable to save profile picture."
            );

            return;
        }

        // -----------------------------------------------------
        // GET OLD PROFILE PICTURE
        // -----------------------------------------------------

        String oldFileName = null;

        String selectSql = """
            SELECT profile_picture
            FROM users
            WHERE user_id = ?
              AND role = 'COUNSELLOR'
            """;

        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                selectSql
                        )
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

            e.printStackTrace();

            deleteFile(
                    newFileName
            );

            sendJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Unable to read current profile picture."
            );

            return;
        }

        // =====================================================
        // UPDATE USERS TABLE
        // =====================================================

        String updateUsersSql = """
            UPDATE users
            SET profile_picture = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ?
              AND role = 'COUNSELLOR'
            """;

        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                updateUsersSql
                        )
        ) {

            ps.setString(
                    1,
                    newFileName
            );

            ps.setLong(
                    2,
                    userId
            );

            int updated =
                    ps.executeUpdate();

            if (updated == 0) {

                deleteFile(
                        newFileName
                );

                sendJson(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        false,
                        "Counsellor account not found."
                );

                return;
            }

        } catch (Exception e) {

            e.printStackTrace();

            deleteFile(
                    newFileName
            );

            sendJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Unable to update profile picture."
            );

            return;
        }

        // =====================================================
        // UPDATE COUNSELLORS TABLE
        // =====================================================

        String updateCounsellorSql = """
            UPDATE counsellors
            SET profile_picture = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE counsellor_id = ?
            """;

        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                updateCounsellorSql
                        )
        ) {

            ps.setString(
                    1,
                    newFileName
            );

            ps.setLong(
                    2,
                    userId
            );

            ps.executeUpdate();

        } catch (Exception e) {

            /*
             * users.profile_picture is the primary source.
             * Do not delete the new image if this secondary
             * synchronization fails.
             */

            e.printStackTrace();
        }

        // =====================================================
        // DELETE OLD FILE
        // =====================================================

        if (oldFileName != null &&
                !oldFileName.isBlank() &&
                !oldFileName.equals(
                        newFileName
                )) {

            deleteFile(
                    oldFileName
            );
        }

        // =====================================================
        // UPDATE SESSION
        // =====================================================

        if (session != null) {

            session.setAttribute(
                    "profilePicture",
                    newFileName
            );
        }

        // =====================================================
        // SUCCESS
        // =====================================================

        sendJson(
                response,
                HttpServletResponse.SC_OK,
                true,
                "Profile picture updated successfully."
        );
    }


    // =========================================================
    // GET - DISPLAY PROFILE PICTURE
    // =========================================================

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

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

        // -----------------------------------------------------
        // ROLE CHECK
        // -----------------------------------------------------

        String role =
                session != null
                        ? (String) session.getAttribute("role")
                        : null;

        if (role == null ||
                !"COUNSELLOR".equalsIgnoreCase(role)) {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            return;
        }

        // -----------------------------------------------------
        // GET FILE NAME
        // -----------------------------------------------------

        String fileName = null;

        String sql = """
            SELECT profile_picture
            FROM users
            WHERE user_id = ?
              AND role = 'COUNSELLOR'
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

        // -----------------------------------------------------
        // NO IMAGE
        // -----------------------------------------------------

        if (fileName == null ||
                fileName.isBlank()) {

            response.setStatus(
                    HttpServletResponse.SC_NOT_FOUND
            );

            return;
        }

        // -----------------------------------------------------
        // SECURITY CHECK
        // -----------------------------------------------------

        if (!isSafeFileName(
                fileName
        )) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            return;
        }

        Path imagePath =
                uploadDirectory
                        .resolve(fileName)
                        .normalize();

        if (!imagePath.startsWith(
                uploadDirectory.normalize()
        )) {

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

        // -----------------------------------------------------
        // CONTENT TYPE
        // -----------------------------------------------------

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

        response.setHeader(
                "Cache-Control",
                "private, max-age=3600"
        );

        // -----------------------------------------------------
        // SEND IMAGE
        // -----------------------------------------------------

        try (
                InputStream input =
                        Files.newInputStream(
                                imagePath
                        );

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

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // DELETE - REMOVE PROFILE PICTURE
    // =========================================================

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
                    "Counsellor is not logged in."
            );

            return;
        }

        // -----------------------------------------------------
        // ROLE CHECK
        // -----------------------------------------------------

        String role =
                session != null
                        ? (String) session.getAttribute("role")
                        : null;

        if (role == null ||
                !"COUNSELLOR".equalsIgnoreCase(role)) {

            sendJson(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    false,
                    "Access denied."
            );

            return;
        }

        // -----------------------------------------------------
        // GET CURRENT FILE
        // -----------------------------------------------------

        String oldFileName = null;

        String selectSql = """
            SELECT profile_picture
            FROM users
            WHERE user_id = ?
              AND role = 'COUNSELLOR'
            """;

        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                selectSql
                        )
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
                            "Counsellor account not found."
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

        // -----------------------------------------------------
        // REMOVE FROM USERS
        // -----------------------------------------------------

        String updateUsersSql = """
            UPDATE users
            SET profile_picture = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ?
              AND role = 'COUNSELLOR'
            """;

        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                updateUsersSql
                        )
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

        // -----------------------------------------------------
        // REMOVE FROM COUNSELLORS
        // -----------------------------------------------------

        String updateCounsellorSql = """
            UPDATE counsellors
            SET profile_picture = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE counsellor_id = ?
            """;

        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                updateCounsellorSql
                        )
        ) {

            ps.setLong(
                    1,
                    userId
            );

            ps.executeUpdate();

        } catch (Exception e) {

            e.printStackTrace();
        }

        // -----------------------------------------------------
        // DELETE PHYSICAL FILE
        // -----------------------------------------------------

        if (oldFileName != null &&
                !oldFileName.isBlank()) {

            deleteFile(
                    oldFileName
            );
        }

        // -----------------------------------------------------
        // UPDATE SESSION
        // -----------------------------------------------------

        if (session != null) {

            session.setAttribute(
                    "profilePicture",
                    null
            );
        }

        // -----------------------------------------------------
        // SUCCESS
        // -----------------------------------------------------

        sendJson(
                response,
                HttpServletResponse.SC_OK,
                true,
                "Profile picture removed successfully."
        );
    }


    // =========================================================
    // SESSION USER ID
    // =========================================================

    private Long getLoggedInUserId(
            HttpSession session) {

        if (session == null) {
            return null;
        }

        Object value =
                session.getAttribute(
                        "userId"
                );

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        return null;
    }


    // =========================================================
    // ALLOWED IMAGE TYPES
    // =========================================================

    private boolean isAllowedImageType(
            String contentType) {

        if (contentType == null) {
            return false;
        }

        return contentType.equalsIgnoreCase(
                    "image/jpeg"
                )
                || contentType.equalsIgnoreCase(
                    "image/jpg"
                )
                || contentType.equalsIgnoreCase(
                    "image/png"
                )
                || contentType.equalsIgnoreCase(
                    "image/webp"
                )
                || contentType.equalsIgnoreCase(
                    "image/gif"
                );
    }


    // =========================================================
    // GET FILE EXTENSION
    // =========================================================

    private String getExtension(
            String submittedFileName,
            String contentType) {

        if (submittedFileName != null) {

            String cleanName =
                    new File(
                            submittedFileName
                    ).getName();

            int dot =
                    cleanName.lastIndexOf('.');

            if (dot >= 0 &&
                    dot < cleanName.length() - 1) {

                String extension =
                        cleanName
                                .substring(dot)
                                .toLowerCase();

                if (extension.equals(".jpg") ||
                        extension.equals(".jpeg") ||
                        extension.equals(".png") ||
                        extension.equals(".webp") ||
                        extension.equals(".gif")) {

                    return extension;
                }
            }
        }

        if ("image/png".equalsIgnoreCase(
                contentType)) {

            return ".png";
        }

        if ("image/webp".equalsIgnoreCase(
                contentType)) {

            return ".webp";
        }

        if ("image/gif".equalsIgnoreCase(
                contentType)) {

            return ".gif";
        }

        return ".jpg";
    }


    // =========================================================
    // SAFE FILE NAME
    // =========================================================

    private boolean isSafeFileName(
            String fileName) {

        if (fileName == null ||
                fileName.isBlank()) {

            return false;
        }

        return !fileName.contains("/")
                && !fileName.contains("\\")
                && !fileName.contains("..");
    }


    // =========================================================
    // DELETE FILE
    // =========================================================

    private void deleteFile(
            String fileName) {

        try {

            if (!isSafeFileName(
                    fileName
            )) {

                return;
            }

            Path file =
                    uploadDirectory
                            .resolve(fileName)
                            .normalize();

            if (!file.startsWith(
                    uploadDirectory.normalize()
            )) {

                return;
            }

            Files.deleteIfExists(
                    file
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // CONTENT TYPE FALLBACK
    // =========================================================

    private String getContentTypeFromExtension(
            String fileName) {

        String lower =
                fileName.toLowerCase();

        if (lower.endsWith(".png")) {

            return "image/png";
        }

        if (lower.endsWith(".webp")) {

            return "image/webp";
        }

        if (lower.endsWith(".gif")) {

            return "image/gif";
        }

        return "image/jpeg";
    }


    // =========================================================
    // JSON RESPONSE
    // =========================================================

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


    // =========================================================
    // JSON ESCAPE
    // =========================================================

    private String escapeJson(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
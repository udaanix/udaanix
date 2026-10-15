package com.udaanix.siya;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.udaanix.siya.security.SecurityUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "SiyaChatServlet", urlPatterns = "/api/siya/chat")
public class SiyaChatServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ClaudeService claude = new ClaudeService();
    private final ConversationService conversations = new ConversationService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");

        try {
            JsonNode body = mapper.readTree(request.getInputStream());
            String message = body == null ? "" : body.path("message").asText("").trim();
            if (message.isBlank()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Please enter a message.");
                return;
            }
            if (message.length() > 4000) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Message is too long. Please keep it under 4000 characters.");
                return;
            }

            JsonNode profile = body == null ? null : body.path("profile");
            if (profile == null || !profile.isObject()) {
                profile = mapper.createObjectNode();
            } else {
                profile = profile.deepCopy();
            }
            JsonNode careerContext = body == null ? null : body.path("careerContext");
            if (careerContext != null && careerContext.isArray() && profile.isObject()) {
                ((ObjectNode) profile).set("careerContext", careerContext);
            }
            String requestedStudentId = profile.path("studentId").asText("").trim();
            String studentId;
            try { studentId = SecurityUtil.resolveStudentId(request, requestedStudentId); } catch (SecurityException e) { sendError(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage()); return; }
            String conversationId = body.path("conversationId").asText("").trim();
            if (conversationId.isBlank() && !studentId.isBlank()) conversationId = conversations.createConversation(studentId, message);
            if (!conversationId.isBlank() && !studentId.isBlank() && !conversations.conversationBelongsTo(conversationId, studentId)) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "This conversation does not belong to the current student."); return;
            }
            String reply = claude.ask(message, profile);
            if (!conversationId.isBlank()) {
                conversations.saveMessage(conversationId, "USER", message);
                conversations.saveMessage(conversationId, "SIYA", reply);
            }
            ObjectNode result = mapper.createObjectNode();
            result.put("success", true);
            result.put("reply", reply);
            result.put("conversationId", conversationId);
            response.getWriter().write(mapper.writeValueAsString(result));
        } catch (IllegalStateException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_BAD_GATEWAY, "SIYA could not reach the AI service right now. Please try again.");
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        ObjectNode error = mapper.createObjectNode();
        error.put("success", false);
        error.put("error", message);
        response.getWriter().write(mapper.writeValueAsString(error));
    }
}

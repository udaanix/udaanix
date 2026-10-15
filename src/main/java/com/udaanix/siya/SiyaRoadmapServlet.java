package com.udaanix.siya;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "SiyaRoadmapServlet", urlPatterns = "/api/siya/roadmap")
public class SiyaRoadmapServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RoadmapService service = new RoadmapService();

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        try {
            JsonNode body = mapper.readTree(request.getInputStream());
            String goal = body == null ? "" : body.path("goal").asText("").trim();
            if (goal.isBlank()) { error(response, 400, "Please enter a career goal."); return; }
            if (goal.length() > 500) { error(response, 400, "Career goal is too long."); return; }
            JsonNode profile = body.path("profile");
            JsonNode careers = body.path("careerContext");
            JsonNode roadmap = service.generate(goal, profile.isObject() ? profile : mapper.createObjectNode(), careers.isArray() ? careers : mapper.createArrayNode());
            ObjectNode out = mapper.createObjectNode(); out.put("success", true); out.set("roadmap", roadmap);
            response.getWriter().write(mapper.writeValueAsString(out));
        } catch (IllegalStateException e) { error(response, 500, e.getMessage()); }
        catch (Exception e) { e.printStackTrace(); error(response, 502, "SIYA could not generate the roadmap right now. Please try again."); }
    }
    private void error(HttpServletResponse r, int status, String message) throws IOException {
        r.setStatus(status); ObjectNode out = mapper.createObjectNode(); out.put("success", false); out.put("error", message); r.getWriter().write(mapper.writeValueAsString(out));
    }
}

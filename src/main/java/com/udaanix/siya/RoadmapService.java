package com.udaanix.siya;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Generates a structured, personalized career roadmap using Claude. */
public class RoadmapService {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final String MODEL = System.getenv().getOrDefault("SIYA_CLAUDE_MODEL", "claude-sonnet-5");
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    public JsonNode generate(String goal, JsonNode profile, JsonNode careerContext) throws Exception {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("ANTHROPIC_API_KEY is not configured on the server.");

        ObjectNode root = mapper.createObjectNode();
        root.put("model", MODEL);
        root.put("max_tokens", 2600);
        root.put("system", systemPrompt());
        ArrayNode messages = root.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", buildContext(goal, profile, careerContext));

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(API_URL)).timeout(Duration.ofSeconds(75))
                .header("x-api-key", apiKey).header("anthropic-version", API_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root))).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) throw new RuntimeException("Claude API returned HTTP " + res.statusCode());
        JsonNode content = mapper.readTree(res.body()).path("content");
        StringBuilder text = new StringBuilder();
        if (content.isArray()) for (JsonNode b : content) if ("text".equals(b.path("type").asText())) text.append(b.path("text").asText());
        String raw = text.toString().trim();
        if (raw.isBlank()) throw new RuntimeException("Empty roadmap response.");
        raw = stripFences(raw);
        try { return mapper.readTree(raw); }
        catch (Exception e) { throw new RuntimeException("SIYA returned an invalid roadmap format."); }
    }

    private String buildContext(String goal, JsonNode profile, JsonNode careers) throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("goal", goal);
        payload.set("student_profile", profile == null ? mapper.createObjectNode() : profile);
        payload.set("udaanix_career_reference", careers == null ? mapper.createArrayNode() : careers);
        return mapper.writeValueAsString(payload);
    }

    private String systemPrompt() {
        return """
        You are SIYA AI, Udaanix's career guidance assistant. Generate a practical personalized career roadmap.
        Use only the supplied student profile and Udaanix career reference for personalized facts. Do not invent eligibility,
        exams, colleges, fees, salaries or deadlines. Current admission details must be verified with official sources.
        Do not guarantee outcomes and do not claim an assessment determines a student's destiny.
        Return ONLY valid JSON, with this exact top-level shape:
        {"goal":"","summary":"","assumptions":[],"stages":[{"title":"","timeframe":"","objective":"","actions":[],"skills":[],"milestones":[]}],"weekly_plan":[],"next_steps":[],"verification_notes":[]}
        Make stages relevant to the student's current education stage. If important information is missing, put concise assumptions
        in the assumptions array rather than inventing it. Keep the roadmap actionable and age-appropriate.
        """;
    }

    private String stripFences(String s) {
        if (s.startsWith("```")) {
            int first = s.indexOf('\n');
            int last = s.lastIndexOf("```");
            if (first >= 0 && last > first) return s.substring(first + 1, last).trim();
        }
        return s;
    }
}

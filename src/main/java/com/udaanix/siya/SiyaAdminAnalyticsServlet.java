package com.udaanix.siya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.udaanix.siya.security.SecurityUtil;
import java.io.IOException;
import java.sql.*;

/** STEP 10: Admin-only SIYA analytics endpoint. */
@WebServlet(name="SiyaAdminAnalyticsServlet", urlPatterns="/api/siya/admin/analytics")
public class SiyaAdminAnalyticsServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        try {
            if (!isAdmin(req)) { resp.setStatus(401); writeError(resp, "Admin authentication required."); return; }
            ObjectNode out = loadAnalytics();
            out.put("success", true);
            resp.getWriter().write(mapper.writeValueAsString(out));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            writeError(resp, "Analytics could not be loaded right now.");
        }
    }

    private boolean isAdmin(HttpServletRequest req) {
        String role = SecurityUtil.currentRole(req);
        if ("ADMIN".equals(role)) return true;
        // Development-only compatibility. Never enable this in production because a browser-held key can be exposed.
        if (!Boolean.parseBoolean(SecurityUtil.env("SIYA_ALLOW_LEGACY_ADMIN_KEY", "false"))) return false;
        String configured = System.getenv("UDAANIX_ADMIN_ANALYTICS_KEY");
        String supplied = req.getHeader("X-Admin-Key");
        return configured != null && !configured.isBlank() && supplied != null && supplied.equals(configured);
    }

    private ObjectNode loadAnalytics() throws SQLException {
        String host=env("UDAANIX_DB_HOST","localhost"), port=env("UDAANIX_DB_PORT","3306"), db=env("UDAANIX_DB_NAME","udaanix");
        String url="jdbc:mysql://"+host+":"+port+"/"+db+"?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String user=env("UDAANIX_DB_USER","root"), password=env("UDAANIX_DB_PASSWORD","");
        ObjectNode o=mapper.createObjectNode();
        try(Connection c=DriverManager.getConnection(url,user,password)) {
            o.put("totalConversations", scalarLong(c,"SELECT COUNT(*) FROM siya_conversations"));
            o.put("activeStudents", scalarLong(c,"SELECT COUNT(DISTINCT student_id) FROM siya_conversations WHERE student_id IS NOT NULL AND student_id<>''"));
            o.put("totalMessages", scalarLong(c,"SELECT COUNT(*) FROM siya_messages"));
            o.put("roadmapsGenerated", tableCount(c,"siya_roadmaps"));
            o.put("counsellorRequests", tableCount(c,"siya_counsellor_handoffs"));
            o.put("pendingCounsellorRequests", scalarLong(c,"SELECT COUNT(*) FROM siya_counsellor_handoffs WHERE status='PENDING'"));
            o.put("completedCounsellorRequests", scalarLong(c,"SELECT COUNT(*) FROM siya_counsellor_handoffs WHERE status='COMPLETED'"));
            o.set("handoffByStatus", group(c,"SELECT status, COUNT(*) total FROM siya_counsellor_handoffs GROUP BY status ORDER BY total DESC","status"));
            o.set("dailyConversations", daily(c));
            o.set("topConversationTopics", topTopics(c));
        }
        return o;
    }

    private long scalarLong(Connection c,String sql)throws SQLException{try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){return r.next()?r.getLong(1):0;}}
    private long tableCount(Connection c,String table)throws SQLException{
        // Table names are fixed constants in this class, not user input.
        return scalarLong(c,"SELECT COUNT(*) FROM "+table);
    }
    private ArrayNode group(Connection c,String sql,String key)throws SQLException{
        ArrayNode a=mapper.createArrayNode(); try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){while(r.next()){ObjectNode x=mapper.createObjectNode();x.put(key,r.getString(1));x.put("count",r.getLong(2));a.add(x);}} return a;
    }
    private ArrayNode daily(Connection c)throws SQLException{
        ArrayNode a=mapper.createArrayNode();
        String sql="SELECT DATE(created_at) day, COUNT(*) total FROM siya_conversations WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 13 DAY) GROUP BY DATE(created_at) ORDER BY day";
        try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){while(r.next()){ObjectNode x=mapper.createObjectNode();x.put("day",r.getString(1));x.put("count",r.getLong(2));a.add(x);}} return a;
    }
    private ArrayNode topTopics(Connection c)throws SQLException{
        ArrayNode a=mapper.createArrayNode();
        String sql="SELECT title, COUNT(*) total FROM siya_conversations WHERE title IS NOT NULL AND title<>'' GROUP BY title ORDER BY total DESC LIMIT 10";
        try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){while(r.next()){ObjectNode x=mapper.createObjectNode();x.put("topic",r.getString(1));x.put("count",r.getLong(2));a.add(x);}} return a;
    }
    private String env(String k,String f){String v=System.getenv(k);return v==null||v.isBlank()?f:v;}
    private void writeError(HttpServletResponse r,String msg)throws IOException{ObjectNode e=mapper.createObjectNode();e.put("success",false);e.put("error",msg);r.getWriter().write(mapper.writeValueAsString(e));}
}

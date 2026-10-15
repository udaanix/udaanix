package com.udaanix.siya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.udaanix.siya.security.SecurityUtil;
import java.io.IOException;

/** STEP 8: GET conversation list or messages. */
@WebServlet(name="SiyaHistoryServlet", urlPatterns="/api/siya/history/*")
public class SiyaHistoryServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConversationService conversations = new ConversationService();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8"); resp.setContentType("application/json;charset=UTF-8");
        String requested=req.getParameter("studentId");
        String studentId;
        try { studentId=SecurityUtil.resolveStudentId(req, requested); } catch(SecurityException e){error(resp,403,e.getMessage());return;}
        if(studentId==null||studentId.isBlank()){error(resp,400,"studentId is required.");return;}
        String path=req.getPathInfo();
        try {
            ObjectNode out=mapper.createObjectNode(); out.put("success",true);
            if(path!=null && path.startsWith("/messages")) {
                String cid=req.getParameter("conversationId");
                if(cid==null||cid.isBlank()){error(resp,400,"conversationId is required.");return;}
                out.set("messages",conversations.listMessages(cid,studentId));
            } else {
                out.set("conversations",conversations.listConversations(studentId));
            }
            resp.getWriter().write(mapper.writeValueAsString(out));
        } catch(SecurityException e){error(resp,403,e.getMessage());}
          catch(Exception e){e.printStackTrace();error(resp,500,"Conversation history is unavailable right now.");}
    }
    private void error(HttpServletResponse r,int status,String message)throws IOException{r.setStatus(status);ObjectNode e=mapper.createObjectNode();e.put("success",false);e.put("error",message);r.getWriter().write(mapper.writeValueAsString(e));}
}

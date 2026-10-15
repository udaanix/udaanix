package com.udaanix.siya;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.udaanix.siya.security.SecurityUtil;
import java.io.IOException;

/** STEP 9: SIYA -> human counsellor handoff endpoint. */
@WebServlet(name="SiyaHandoffServlet", urlPatterns="/api/siya/counsellor-handoff")
public class SiyaHandoffServlet extends HttpServlet {
    private final ObjectMapper mapper=new ObjectMapper();
    private final CounsellorHandoffService service=new CounsellorHandoffService();
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        resp.setCharacterEncoding("UTF-8");resp.setContentType("application/json;charset=UTF-8");
        try{
            JsonNode b=mapper.readTree(req.getInputStream());
            String studentId=b.path("studentId").asText("").trim(), cid=b.path("conversationId").asText("").trim();
            String reason=b.path("reason").asText("").trim(), mode=b.path("preferredMode").asText("ANY").trim();
            if(reason.length()>2000)reason=reason.substring(0,2000);
            ObjectNode out=service.create(studentId,cid,reason,mode);out.put("success",true);
            out.put("message","Your request has been sent to the Udaanix counselling team.");resp.getWriter().write(mapper.writeValueAsString(out));
        }catch(IllegalArgumentException e){error(resp,400,e.getMessage());}catch(Exception e){e.printStackTrace();error(resp,500,"Counsellor request could not be created right now.");}
    }
    private void error(HttpServletResponse r,int s,String m)throws IOException{r.setStatus(s);ObjectNode e=mapper.createObjectNode();e.put("success",false);e.put("error",m);r.getWriter().write(mapper.writeValueAsString(e));}
}

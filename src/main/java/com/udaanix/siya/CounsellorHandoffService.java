package com.udaanix.siya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.*;
import java.util.UUID;

/** STEP 9: Creates a human-counsellor handoff request for a student. */
public class CounsellorHandoffService {
    private final String url, user, password;
    public CounsellorHandoffService() {
        String host=env("UDAANIX_DB_HOST","localhost"), port=env("UDAANIX_DB_PORT","3306"), db=env("UDAANIX_DB_NAME","udaanix");
        url="jdbc:mysql://"+host+":"+port+"/"+db+"?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        user=env("UDAANIX_DB_USER","root"); password=env("UDAANIX_DB_PASSWORD","");
    }
    private String env(String k,String f){String v=System.getenv(k);return v==null||v.isBlank()?f:v;}
    private Connection connection() throws SQLException{return DriverManager.getConnection(url,user,password);}

    public ObjectNode create(String studentId,String conversationId,String reason,String mode) throws SQLException {
        if(studentId==null||studentId.isBlank()) throw new IllegalArgumentException("Please log in before requesting a counsellor.");
        String id=UUID.randomUUID().toString();
        String sql="INSERT INTO siya_counsellor_handoffs (handoff_id,student_id,conversation_id,reason,preferred_mode,status) VALUES (?,?,?,?,?,'PENDING')";
        try(Connection c=connection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,id); ps.setString(2,studentId); if(conversationId==null||conversationId.isBlank())ps.setNull(3,Types.VARCHAR);else ps.setString(3,conversationId);
            ps.setString(4,reason==null?"":reason.substring(0,Math.min(reason.length(),2000))); ps.setString(5,mode==null||mode.isBlank()?"ANY":mode.toUpperCase()); ps.executeUpdate();
        }
        ObjectMapper m=new ObjectMapper(); ObjectNode out=m.createObjectNode(); out.put("handoffId",id); out.put("status","PENDING"); return out;
    }
}

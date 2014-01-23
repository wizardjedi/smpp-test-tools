package com.a1systems.http.adapter.servlet;

import com.a1systems.http.adapter.Application;
import com.a1systems.http.adapter.message.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class DeliveryStatusServlet extends HttpServlet {

    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected Application application;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        
        String msgId = req.getParameter("id");
        
        Message msg = application.getMessages().get(Long.valueOf(msgId));
        
        response.getWriter().println(objectMapper.writeValueAsString(msg));
    }
}

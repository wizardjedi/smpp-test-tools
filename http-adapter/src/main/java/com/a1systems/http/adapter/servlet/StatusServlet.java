package com.a1systems.http.adapter.servlet;

import com.a1systems.http.adapter.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class StatusServlet extends HttpServlet {
    @Autowired
    protected Application application;

    @Autowired
    protected ObjectMapper objectMapper;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        
        String data = objectMapper.writeValueAsString(new ArrayList<String>());
        
        response.getWriter().println(data);
    }

}

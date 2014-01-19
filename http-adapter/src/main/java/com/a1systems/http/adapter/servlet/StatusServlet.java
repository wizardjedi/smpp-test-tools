package com.a1systems.http.adapter.servlet;

import com.a1systems.http.adapter.Application;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class StatusServlet extends HttpServlet {
    @Autowired
    protected Application application;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("Status"+application.getMessageParts().keySet().toString()+application.getLinkIds().toString()+application.getMessages().keySet().toString());
    }

}

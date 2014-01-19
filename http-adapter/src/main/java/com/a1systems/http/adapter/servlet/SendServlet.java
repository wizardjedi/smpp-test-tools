package com.a1systems.http.adapter.servlet;

import com.a1systems.http.adapter.Application;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SendServlet extends HttpServlet {

    @Autowired
    protected Application application;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

        String source = req.getParameter("source");
        String destination = req.getParameter("destination");
        String text = req.getParameter("text");
        String encoding = req.getParameter("encoding");
        String link = req.getParameter("link");


        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("Send"+this.application.sendMessage(link, source, destination, text, encoding));
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

}

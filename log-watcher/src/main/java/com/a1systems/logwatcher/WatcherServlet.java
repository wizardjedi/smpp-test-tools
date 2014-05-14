package com.a1systems.logwatcher;

import java.io.IOException;
import java.io.InputStream;
import javax.print.DocFlavor;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class WatcherServlet extends HttpServlet {
    protected static final Logger logger = LoggerFactory.getLogger(WatcherServlet.class);

    @Autowired
    protected Application application;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        if (req.getRequestURI().startsWith("/watcher")) {
            processWatcher(req, response);
        } else {
            processIndex(req, response);
        }
    }

    protected void processWatcher(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8;");
        response.setStatus(HttpServletResponse.SC_OK);

        response.getWriter().println(application.processCommand(req.getParameter("cmd")));
    }

    protected void processIndex(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8;");
        response.setStatus(HttpServletResponse.SC_OK);

        InputStream indexStream = getClass().getResourceAsStream("/index.html");

        while (indexStream.available()>0) {
            byte[] buffer = new byte[64*1024];

            indexStream.read(buffer);

            response.getWriter().print(new String(buffer));
        }

    }


}
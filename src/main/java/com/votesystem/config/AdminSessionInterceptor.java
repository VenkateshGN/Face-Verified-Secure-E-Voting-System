package com.votesystem.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminSessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin/login") || uri.startsWith("/admin/logout") || uri.startsWith("/admin/register-organizer") || uri.startsWith("/admin/css") || uri.startsWith("/admin/js")) {
            return true;
        }

        HttpSession session = request.getSession();
        if (session.getAttribute("adminId") == null) {
            response.sendRedirect("/admin/login");
            return false;
        }
        return true;
    }
}

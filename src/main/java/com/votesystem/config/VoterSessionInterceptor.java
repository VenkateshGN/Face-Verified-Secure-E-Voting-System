package com.votesystem.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VoterSessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession();
        Object voterId = session.getAttribute("voterId");
        String step = (String) session.getAttribute("step");

        if (uri.startsWith("/verify-face")) {
            if (voterId == null) {
                response.sendRedirect("/login");
                return false;
            }
        } else if (uri.startsWith("/verify-otp")) {
            if (voterId == null) {
                response.sendRedirect("/login");
                return false;
            }
            if (!"STEP_OTP".equals(step) && !"STEP_BALLOT".equals(step) && !"DONE".equals(step)) {
                response.sendRedirect("/verify-face");
                return false;
            }
        } else if (uri.startsWith("/ballot")) {
            if (voterId == null) {
                response.sendRedirect("/login");
                return false;
            }
            if (!"STEP_BALLOT".equals(step) && !"DONE".equals(step)) {
                response.sendRedirect("/verify-otp");
                return false;
            }
        }
        return true;
    }
}

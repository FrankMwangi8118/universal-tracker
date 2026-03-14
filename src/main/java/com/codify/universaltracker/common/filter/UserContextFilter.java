package com.codify.universaltracker.common.filter;

import com.codify.universaltracker.common.util.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class UserContextFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(UserContextFilter.class);
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String userIdHeader = httpRequest.getHeader(USER_ID_HEADER);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdHeader.trim());
                UserContext.set(userId);
                log.debug("UserContext set for user: {}", userId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid X-User-Id header value: {}", userIdHeader);
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                httpResponse.getWriter().write("{\"error\":\"Invalid X-User-Id header\"}");
                return;
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}

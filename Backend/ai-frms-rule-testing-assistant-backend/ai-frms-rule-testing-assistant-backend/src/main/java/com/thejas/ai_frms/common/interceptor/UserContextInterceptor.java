package com.thejas.ai_frms.common.interceptor;

import com.thejas.ai_frms.common.util.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Reads the X-Actor-Username request header and stores it in UserContextHolder
 * for use by AuditLogServiceImpl.
 *
 * The JWT filter (JwtAuthenticationFilter) already populates UserContextHolder from
 * the JWT token before this interceptor runs. This interceptor only applies the
 * header value as a fallback for clients that send the header but no Bearer token
 * (e.g. legacy clients or direct Swagger calls without auth).
 *
 * The ThreadLocal is always cleared in afterCompletion to prevent leaks.
 */
public class UserContextInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(UserContextInterceptor.class);
    public static final String HEADER_NAME = "X-Actor-Username";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // JWT filter already set the username — honour it, skip the header
        if (UserContextHolder.getUsername() != null) {
            return true;
        }
        String username = request.getHeader(HEADER_NAME);
        if (username != null && !username.isBlank()) {
            UserContextHolder.setUsername(username.trim());
            log.debug("[AUDIT] fallback actor={} from {} header", username.trim(), HEADER_NAME);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
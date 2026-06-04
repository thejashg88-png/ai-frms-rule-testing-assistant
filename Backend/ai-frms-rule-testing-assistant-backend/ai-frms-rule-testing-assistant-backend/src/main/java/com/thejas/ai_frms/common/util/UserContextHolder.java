package com.thejas.ai_frms.common.util;

/**
 * ThreadLocal holder for the username of the user making the current HTTP request.
 * Populated by UserContextInterceptor from the X-Actor-Username header.
 * Must be cleared after each request to avoid ThreadLocal leaks in thread pools.
 */
public final class UserContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void setUsername(String username) {
        CONTEXT.set(username);
    }

    public static String getUsername() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
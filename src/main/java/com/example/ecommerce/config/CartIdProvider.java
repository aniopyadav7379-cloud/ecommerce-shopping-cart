package com.example.ecommerce.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves a stable cart identifier for guest shoppers.
 *
 * A long-lived cookie (rather than the raw HTTP session id) is used so the
 * cart survives session invalidation/renegotiation and so cart state is
 * never held only in JavaScript — every add/update/remove is persisted in
 * the database against this id immediately.
 */
@Component
public class CartIdProvider {

    public static final String COOKIE_NAME = "CART_ID";
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30; // 30 days

    public String resolveCartId(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        String newCartId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(COOKIE_NAME, newCartId);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return newCartId;
    }
}

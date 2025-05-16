package com.fptu.sep490.commonlibrary.utils;

import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public class CookieUtils {
    public static void setTokenCookies(HttpServletResponse response, KeyCloakTokenResponse tokenResponse) {
        ResponseCookie accessCookie = ResponseCookie.from("Authorization", tokenResponse.accessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(tokenResponse.expiresIn())
                .sameSite("Strict")
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokenResponse.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(tokenResponse.expiresIn())
                .sameSite("Strict")
                .build();


        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public static void clearCookie(HttpServletResponse response) {
        Cookie accessToken = new Cookie("Authorization", null);
        accessToken.setMaxAge(0);
        accessToken.setPath("/");

        Cookie refreshToken = new Cookie("refresh_token", null);
        refreshToken.setMaxAge(0);
        refreshToken.setPath("/");

        response.addCookie(accessToken);
        response.addCookie(refreshToken);
    }

    public static String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

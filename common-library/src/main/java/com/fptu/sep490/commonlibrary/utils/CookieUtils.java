package com.fptu.sep490.commonlibrary.utils;

import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtils {
    public static void setTokenCookies(HttpServletResponse response, KeyCloakTokenResponse tokenResponse) {
        Cookie accessToken = new Cookie("authorization", tokenResponse.accessToken());
        accessToken.setHttpOnly(true);
        accessToken.setPath("/");
        accessToken.setMaxAge(tokenResponse.expiresIn());

        Cookie refreshToken = new Cookie("refresh_token", tokenResponse.refreshToken());
        refreshToken.setHttpOnly(true);
        refreshToken.setPath("/");
        refreshToken.setMaxAge(tokenResponse.refreshExpiresIn());

        response.addCookie(accessToken);
        response.addCookie(refreshToken);
    }

    public static void clearCookie(HttpServletResponse response) {
        Cookie accessToken = new Cookie("authorization", null);
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

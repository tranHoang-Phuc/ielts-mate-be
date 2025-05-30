package com.fptu.sep490.readingservice.component;

import com.fptu.sep490.commonlibrary.constants.CookieConstants;
import com.fptu.sep490.commonlibrary.exceptions.AccessDeniedException;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.commonlibrary.viewmodel.response.KeyCloakTokenResponse;

import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthTokenInterceptor implements HandlerInterceptor {

    ObjectProvider<KeyCloakTokenClient> keyCloakTokenClient;
    @Value("${keycloak.realm}")
    @NonFinal
    String realm;

    @Value("${keycloak.client-id}")
    @NonFinal
    String clientId;

    @Value("${keycloak.client-secret}")
    @NonFinal
    String clientSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String accessToken = CookieUtils.getCookieValue(request, CookieConstants.ACCESS_TOKEN);
        String refreshToken = CookieUtils.getCookieValue(request, CookieConstants.REFRESH_TOKEN);
        if(accessToken == null || refreshToken == null) {
            return true;
        }
        boolean isValid = validateAccessToken(accessToken);
        if (isValid) return true;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        try {
            KeyCloakTokenResponse keyCloakTokenResponse = keyCloakTokenClient.getObject().requestToken(form, realm);
            CookieUtils.setTokenCookies(response, keyCloakTokenResponse);
        } catch (Exception ex) {
            throw new AccessDeniedException(Constants.ErrorCode.UNAUTHORIZED, Constants.ErrorCodeMessage.UNAUTHORIZED);
        }
        return true;
    }

    private boolean validateAccessToken(String accessToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("token", accessToken);
        var response = keyCloakTokenClient.getObject().introspect(realm, form);
        return response.isActive();
    }
}

package com.fptu.sep490.identityservice.constants;

public class Constants {
    public final class ErrorCode {
        public static final String ACCESS_DENIED = "ACCESS_DENIED";
        public static final String SIGN_IN_REQUIRE_EXCEPTION = "SIGN_IN_REQUIRE_EXCEPTION";
        public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String KEYCLOAK_ERROR = "KEYCLOAK_ERROR";
        public static final String EXISTED_USERNAME = "EXISTED_USERNAME";
        public static final String EXISTED_EMAIL = "EXISTED_EMAIL";
        public static final String USERNAME_MISSING = "USERNAME_MISSING";
    }

    public final class RedisKey {
        public static final String KEY_CLOAK_CLIENT_TOKEN = "keycloak-client-token";
    }
}

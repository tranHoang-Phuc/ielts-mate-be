package com.fptu.sep490.identityservice.constants;

public class Constants {
    public final class ErrorCodeMessage {
        public static final String ACCESS_DENIED = "ACCESS_DENIED";
        public static final String SIGN_IN_REQUIRE_EXCEPTION = "SIGN_IN_REQUIRE_EXCEPTION";
        public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String KEYCLOAK_ERROR = "KEYCLOAK_ERROR";
        public static final String EXISTED_USERNAME = "EXISTED_USERNAME";
        public static final String EXISTED_EMAIL = "EXISTED_EMAIL";
        public static final String USERNAME_MISSING = "USERNAME_MISSING";
        public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
        public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
        public static final String INVALID_VERIFIED_TOKEN = "INVALID_VERIFIED_TOKEN";
        public static final String CONFLICT_PASSWORD = "CONFLICT_PASSWORD";
    }

    public final class RedisKey {
        public static final String KEY_CLOAK_CLIENT_TOKEN = "keycloak-client-token";
        public static final String USER_PENDING_VERIFY = "keycloak-client-refresh-token";
    }

    public final class ErrorCode {
        public static final String KEYCLOAK_ERROR = "00010";
        public static final String EXISTED_USERNAME = "00011";
        public static final String EXISTED_EMAIL = "00012";
        public static final String USERNAME_MISSING = "00013";

        public static final String UNAUTHORIZED = "00014";
        public static final String SIGN_IN_REQUIRE_EXCEPTION = "00015";

        public static final String INVALID_VERIFIED_TOKEN = "00016";

        public static final String CONFLICT_PASSWORD = "00017";
        public static final String USER_NOT_FOUND = "00018";
    }
}

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
        public static final String TIME_OUT_TOKEN = "TIME_OUT_TOKEN";
        public static final String EMAIL_NOT_MATCH = "EMAIL_NOT_MATCH";

        public static final String FORGOT_PASSWORD_RATE_LIMIT = "FORGOT_PASSWORD_RATE_LIMIT";
        public static final String REDIS_ERROR = "REDIS_ERROR";

        public static final String VERIFY_EMAIL_RATE_LIMIT = "VERIFY_EMAIL_RATE_LIMIT";
        public static final String TOO_MANY_REQUEST = "TOO_MANY_REQUEST";
        public static final String EMAIL_ALREADY_VERIFIED = "EMAIL_ALREADY_VERIFIED";
        public static final String EMAIL_NOT_SETUP = "EMAIL_NOT_SETUP";
        public static final String ACCOUNT_NOT_EXIST = "ACCOUNT_NOT_EXIST";
        public static final String WRONG_PASSWORD = "WRONG_PASSWORD";
        public static final String WRONG_OLD_PASSWORD = "WRONG_OLD_PASSWORD";
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
        public static final String TIME_OUT_TOKEN = "00019";
        public static final String EMAIL_NOT_MATCH = "00020";
        public static final String FORGOT_PASSWORD_RATE_LIMIT = "00021";

        public static final String REDIS_ERROR = "00022";
        public static final String VERIFY_EMAIL_RATE_LIMIT = "00023";

        public static final String TOO_MANY_REQUEST = "00024";


        public static final String EMAIL_ALREADY_VERIFIED = "00025";
        public static final String EMAIL_NOT_SETUP = "00026";
        public static final String ACCOUNT_NOT_EXIST = "00027";
        public static final String EMAIL_NOT_VERIFIED = "00028";

        public static final String WRONG_PASSWORD = "00029";

        public static final String WRONG_OLD_PASSWORD = "00030";


    }
}

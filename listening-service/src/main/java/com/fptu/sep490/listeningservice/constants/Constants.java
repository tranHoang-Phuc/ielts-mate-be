package com.fptu.sep490.listeningservice.constants;

public class Constants {
    public final class ErrorCodeMessage {
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
    }

    public final class RedisKey {
        public static final String KEY_CLOAK_CLIENT_TOKEN = "keycloak-client-token";
        public static final String USER_PENDING_VERIFY = "keycloak-client-refresh-token";
    }

    public final class ErrorCode {


        public static final String UNAUTHORIZED = "400001";

    }
}

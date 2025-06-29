package com.fptu.sep490.fileservice.constants;

public class Constants {
    public final class ErrorCodeMessage {
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    }

    public final class RedisKey {
        public static final String KEY_CLOAK_CLIENT_TOKEN = "keycloak-client-token";
        public static final String USER_PENDING_VERIFY = "keycloak-client-refresh-token";
    }

    public final class ErrorCode {


        public static final String UNAUTHORIZED = "300001";
        public static final String FILE_NOT_FOUND = "300002";

    }
}

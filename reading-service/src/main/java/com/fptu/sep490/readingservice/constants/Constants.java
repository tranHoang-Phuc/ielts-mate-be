package com.fptu.sep490.readingservice.constants;

public class Constants {
    public final class ErrorCodeMessage {
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String INVALID_REQUEST = "INVALID_REQUEST";
        public static final String PASSAGE_NOT_FOUND = "PASSAGE_NOT_FOUND";
        public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
        public static final String QUESTION_GROUP_NOT_FOUND = "QUESTION_GROUP_NOT_FOUND";
    }

    public final class RedisKey {

        public static final String KEY_CLOAK_CLIENT_TOKEN = "keycloak-client-token";
    }

    public final class ErrorCode {

        public static final String UNAUTHORIZED = "100001";
        public static final String INVALID_REQUEST = "100002";
        public static final String PASSAGE_NOT_FOUND = "100003";
        public static final String QUESTION_GROUP_NOT_FOUND = "100004";
        public static final String INTERNAL_SERVER_ERROR = "100999";
    }
}

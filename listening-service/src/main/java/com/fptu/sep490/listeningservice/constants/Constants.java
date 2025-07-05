package com.fptu.sep490.listeningservice.constants;

public class Constants {
    public final class ErrorCodeMessage {
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String INVALID_REQUEST = "INVALID_REQUEST";
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String ERROR_WHEN_UPLOAD = "ERROR_WHEN_UPLOAD";
        public static final String LISTENING_TASK_NOT_ACTIVATED = "LISTENING_TASK_NOT_ACTIVATED";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String ATTEMPT_NOT_DRAFT = "ATTEMPT_NOT_DRAFT";
        public static final String QUESTION_NOT_FOUND = "QUESTION_NOT_FOUND";
        public static final String INVALID_QUESTION_TYPE = "INVALID_QUESTION_TYPE";
    }

    public final class RedisKey {
        public static final String KEY_CLOAK_CLIENT_TOKEN = "keycloak-client-token";
        public static final String USER_PENDING_VERIFY = "keycloak-client-refresh-token";
        public static final String USER_PROFILE = "user-profile";
    }

    public final class ErrorCode {


        public static final String UNAUTHORIZED = "400001";
        public static final String INVALID_REQUEST = "400002";
        public static final String NOT_FOUND = "400003";
        public static final String ERROR_WHEN_UPLOAD="400004";
        public static final String LISTENING_TASK_NOT_ACTIVATED = "400005";
        public static final String FORBIDDEN = "400006";
        public static final String ATTEMPT_NOT_DRAFT = "400007";

        public static final String QUESTION_NOT_FOUND = "400008";
        public static final String INVALID_QUESTION_TYPE = "400009";

    }
}

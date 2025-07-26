package com.fptu.sep490.personalservice.constants;

public class Constants {
    public final class ErrorCodeMessage {
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String FORBIDDEN="FORBIDDEN";


        public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
        public static final String INVALID_REQUEST = "INVALID_REQUEST";
        public static final String REMINDER_CONFIGURED = "REMINDER_CONFIGURED";
        public static final String REMINDER_NOT_FOUND = "REMINDER_NOT_FOUND";
        public static final String MARK_UP_NOT_FOUND = "MARK_UP_NOT_FOUND";

        //vocabulary
        public static final String VOCABULARY_ALREADY_EXISTS = "VOCABULARY_ALREADY_EXISTS";
    }

    public final class RedisKey {

        public static final String USER_PROFILE = "user-profile";
        public static final String KEY_CLOAK_CLIENT_TOKEN = "keycloak-client-token";
    }

    public final class ErrorCode {
        public static final String UNAUTHORIZED = "500001";
        public static final String NOT_FOUND = "500002";
        public static final String INTERNAL_SERVER_ERROR = "999999";
        public static final String INVALID_REQUEST = "500002";
        public static final String REMINDER_CONFIGURED = "500003";
        public static final String REMINDER_NOT_FOUND = "500004";
        public static final String MARK_UP_NOT_FOUND = "500005";
        public static final String FORBIDDEN = "500006";

        //vocabulary
        public static final String VOCABULARY_ALREADY_EXISTS = "500006";


    }

    public final class Config {
        public static final String TARGET_CONFIG = "target_config";
        public static final String REMINDER_CONFIG = "reminder_config";
        public static final String STREAK_CONFIG = "streak_config";
    }

    public final class Streak {
        public static final int TPL_3 = 3;
        public static final int TPL_10 = 10;
        public static final int TPL_30 = 30;
        public static final int TPL_90 = 90;
    }

    public final class StreakMessage {
        public static final String[] TPL_3 = {
                "Nice! You've hit a %d-day streak—3-day milestone unlocked!",
                "Keep it rolling: %d days straight! First 3-day mark achieved."
        };
        public static final String[] TPL_10 = {
                "Awesome! %d days in a row—10-day milestone!",
                "Double digits! Congrats on your 10-day streak (%d days)."
        };
        public static final String[] TPL_30 = {
                "Impressive: %d days of continuous learning—30-day milestone!",
                "1 month strong! %d-day streak reached."
        };
        public static final String[] TPL_90 = {
                "Phenomenal! %d days—90-day milestone. You’re unstoppable!",
                "Quarter‑year streak! %d days straight of learning. Bravo!"
        };
    }
}

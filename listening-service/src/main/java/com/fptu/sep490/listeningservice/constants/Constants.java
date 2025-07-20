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
        public static final String ATTEMPT_ALREADY_SUBMITTED = "ATTEMPT_ALREADY_SUBMITTED";
        public static final String ATTEMPT_NOT_FINISHED = "ATTEMPT_NOT_FINISHED";
        public static final String QUESTION_LIST_EMPTY = "QUESTION_LIST_EMPTY";
        public static final String QUESTION_GROUP_NOT_FOUND = "QUESTION_GROUP_NOT_FOUND";
        public static final String CHOICES_LIST_EMPTY = "CHOICES_LIST_EMPTY";
        public static final String INVALID_NUMBER_OF_CORRECT_ANSWERS = "INVALID_NUMBER_OF_CORRECT_ANSWERS";
        public static final String INVALID_BLANK_INDEX = "INVALID_BLANK_INDEX";
        public static final String QUESTION_NOT_BELONG_TO_GROUP = "QUESTION_NOT_BELONG_TO_GROUP";
        public static final String DRAG_ITEM_NOT_FOUND = "DRAG_ITEM_NOT_FOUND";
        public static final String WRONG_PART = "WRONG_PART";
        public static final String EXAM_DELETED = "EXAM_DELETED";
        public static final String LISTENING_EXAM_NOT_FOUND = "LISTENING_EXAM_NOT_FOUND";
        public static final String LISTENING_TASK_NOT_FOUND = "LISTENING_TASK_NOT_FOUND";
        public static final String CHOICE_NOT_FOUND = "CHOICE_NOT_FOUND";
        public static final String EXAM_ATTEMPT_NOT_FOUND = "EXAM_ATTEMPT_NOT_FOUND";

        public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
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
        public static final String ATTEMPT_ALREADY_SUBMITTED = "400010";
        public static final String ATTEMPT_NOT_FINISHED = "400011";

        public static final String QUESTION_LIST_EMPTY = "400012";
        public static final String QUESTION_GROUP_NOT_FOUND = "400013";
        public static final String CHOICES_LIST_EMPTY = "400014";
        public static final String INVALID_NUMBER_OF_CORRECT_ANSWERS = "400015";
        public static final String INVALID_BLANK_INDEX = "400016";
        public static final String QUESTION_NOT_BELONG_TO_GROUP = "400017";
        public static final String DRAG_ITEM_NOT_FOUND = "400018";
        public static final String WRONG_PART = "400019";
        public static final String EXAM_DELETED = "400020";

        public static final String LISTENING_EXAM_NOT_FOUND = "400021";
        public static final String LISTENING_TASK_NOT_FOUND = "400022";
        public static final String CHOICE_NOT_FOUND = "400023";
        public static final String EXAM_ATTEMPT_NOT_FOUND = "400024";

        public static final String INTERNAL_SERVER_ERROR = "400999";

    }
}

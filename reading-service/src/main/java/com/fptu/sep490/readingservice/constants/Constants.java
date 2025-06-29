package com.fptu.sep490.readingservice.constants;

public class Constants {
    public final class ErrorCodeMessage {
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final String INVALID_REQUEST = "INVALID_REQUEST";
        public static final String PASSAGE_NOT_FOUND = "PASSAGE_NOT_FOUND";
        public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
        public static final String INVALID_NUMBER_OF_CORRECT_ANSWERS = "INVALID_NUMBER_OF_CORRECT_ANSWERS";
        public static final String QUESTION_LIST_EMPTY = "QUESTION_LIST_EMPTY";
        public static final String INVALID_QUESTION_TYPE = "INVALID_QUESTION_TYPE";
        public static final String CHOICES_LIST_EMPTY = "CHOICES_LIST_EMPTY";
        public static final String INVALID_BLANK_INDEX = "INVALID_BLANK_INDEX";
        public static final String QUESTION_GROUP_NOT_FOUND = "QUESTION_GROUP_NOT_FOUND";
        public static final String READING_EXAM_NOT_FOUND = "READING_EXAM_NOT_FOUND";
        public static final String QUESTION_NOT_FOUND = "QUESTION_NOT_FOUND";
        public static final String QUESTION_ALREADY_HAS_DRAG_ITEM = "QUESTION_ALREADY_HAS_DRAG_ITEM";
        public static final String DRAG_ITEM_NOT_FOUND = "DRAG_ITEM_NOT_FOUND";
        public static final String CHOICE_NOT_FOUND = "CHOICE_NOT_FOUND";
        public static final String CHOICE_NOT_IN_THIS_QUESTION = "CHOICE_NOT_IN_THIS_QUESTION";
        public static final String QUESTION_NOT_BELONG_TO_GROUP = "QUESTION_NOT_BELONG_TO_GROUP";
        public static final String INVALID_INPUT = "INVALID_INPUT";

        public static final String PASSAGE_NOT_ACTIVE = "PASSAGE_NOT_ACTIVE";
        public static final String ATTEMPT_NOT_FOUND = "ATTEMPT_NOT_FOUND";
        public static final String FORBIDDEN = "FORBIDDEN";
        public static final String ATTEMPT_NOT_DRAFT = "ATTEMPT_NOT_DRAFT";
        public static final String ATTEMPT_ALREADY_SUBMITTED = "ATTEMPT_ALREADY_SUBMITTED";
    }

    public final class RedisKey {

        public static final String KEY_CLOAK_CLIENT_TOKEN = "keycloak-client-token";
        public static final String USER_PROFILE = "user-profile";
    }

    public final class ErrorCode {

        public static final String UNAUTHORIZED = "100001";
        public static final String INVALID_REQUEST = "100002";
        public static final String PASSAGE_NOT_FOUND = "100003";
        public static final String INVALID_NUMBER_OF_CORRECT_ANSWERS = "100004";
        public static final String QUESTION_LIST_EMPTY = "100005";
        public static final String INVALID_QUESTION_TYPE = "100006";
        public static final String CHOICES_LIST_EMPTY = "100007";
        public static final String INVALID_BLANK_INDEX = "100008";
        public static final String QUESTION_GROUP_NOT_FOUND = "100009";
        public static final String CHOICE_NOT_FOUND = "100010";
        public static final String CHOICE_NOT_IN_THIS_QUESTION = "100011";
        public static final String QUESTION_NOT_BELONG_TO_GROUP = "100012";
        public  static final String PASSAGE_NOT_ACTIVE = "100013";
        public static final String ATTEMPT_NOT_FOUND = "100014";
        public static final String FORBIDDEN = "100015";
        public static final String ATTEMPT_NOT_DRAFT = "100016";



        public static final String QUESTION_NOT_FOUND = "100021";
        public static final String QUESTION_ALREADY_HAS_DRAG_ITEM = "100022";
        public static final String DRAG_ITEM_NOT_FOUND = "100023";
        public static final String READING_EXAM_NOT_FOUND="100024";
        public static final String INVALID_INPUT = "100025";
        public static final String INTERNAL_SERVER_ERROR = "100999";

        public static final String ATTEMPT_ALREADY_SUBMITTED = "100026";
    }
}

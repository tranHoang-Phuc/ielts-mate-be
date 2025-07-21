package com.fptu.sep490.readingservice.model.enumeration;

public enum QuestionType {
    MULTIPLE_CHOICE,
    FILL_IN_THE_BLANKS,
    MATCHING,
    DRAG_AND_DROP;
    public static QuestionType fromValue(int value) {
        return QuestionType.values()[value];
    }
}

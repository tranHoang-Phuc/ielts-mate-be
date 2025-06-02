package com.fptu.sep490.readingservice.Dto;
import java.util.List;

public class QuestionDto {
    private int question_order;
    private int point;
    private int question_type;
    private List<String> question_category;
    private String explanation;
    private int number_of_correct_answer;
    private List<ChoiceDto> choices;
    private String instruction_for_choice;
    private int blank_index;
    private String correct_answer;
    private String instruction_for_matching;
    private String correct_answer_for_matching;
    private int zone_index;
    private String drag_item;
}
